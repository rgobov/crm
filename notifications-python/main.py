from __future__ import annotations

from fastapi import FastAPI, HTTPException, BackgroundTasks, Header, Request
from contextlib import asynccontextmanager
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from pydantic_settings import BaseSettings
from pydantic import ConfigDict
import os
import asyncio
import logging
import shutil
import qrcode
from io import BytesIO
import base64
from typing import Optional, Dict, Set
from datetime import datetime, timedelta
from pyrogram import Client
from pyrogram.types import User, InputPhoneContact
import httpx
import traceback

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    logger.info("Starting notifications-python service...")
    asyncio.create_task(warmup_clients())
    yield
    # Shutdown
    global is_stopping
    is_stopping = True
    
    for tenant_id, client_wrapper in active_clients.items():
        try:
            await client_wrapper.stop()
        except Exception as e:
            logger.error(f"Failed to stop client for {tenant_id}: {e}")
    
    await http_client.aclose()

app = FastAPI(title="Notifications Service (Python)", lifespan=lifespan)

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Configuration
class Settings(BaseSettings):
    telegram_api_id: int
    telegram_api_hash: str
    backend_url: str = "http://backend:8080"
    internal_secret: str = "try-neuro-internal-secret-2026"
    sessions_path: str = "./sessions"
    telegram_proxy: Optional[str] = None

    model_config = ConfigDict(
        env_file='.env',
        env_file_encoding='utf-8',
        extra='ignore'
    )

settings = Settings()

# Storage (async-safe)
active_clients: Dict[str, "TelegramClientWrapper"] = {}
pending_qr_links: Dict[str, str] = {}
last_synced_qr_link: Dict[str, str] = {}
flood_wait_until: Dict[str, datetime] = {}
session_locks: Dict[str, asyncio.Lock] = {}
pending_closes: Dict[str, asyncio.Future] = {}
tenants_to_cleanup: Set[str] = set()
is_stopping = False
session_locks_lock = asyncio.Lock()

# HTTP client for backend communication
http_client = httpx.AsyncClient(timeout=30.0)

class TelegramClientWrapper:
    def __init__(self, tenant_id: str):
        self.tenant_id = tenant_id
        self.client: Optional[Client] = None
        # Use absolute path to avoid SQLite "unable to open database file" errors
        self.session_path = os.path.abspath(os.path.join(settings.sessions_path, tenant_id))
        self.is_ready = False
        self.is_authorized = False
        self.phone_code_hash = None
        self.phone_number = None
        self.auth_state = "DISCONNECTED"  # DISCONNECTED, WAITING_CODE, WAITING_PASSWORD, CONNECTED
        
    async def start(self):
        os.makedirs(self.session_path, exist_ok=True)
        # Session name should not be the same as the directory path to avoid SQLite errors
        session_name = os.path.join(self.session_path, "tg_session")

        # Parse proxy if provided
        proxy = None
        if settings.telegram_proxy:
            try:
                # Support for proxy string parsing
                from urllib.parse import urlparse
                url = urlparse(settings.telegram_proxy)

                # Tinyproxy is usually HTTP
                scheme = url.scheme or "http"
                hostname = url.hostname or (url.path.split(':')[0] if ':' in url.path else url.path)
                try:
                    port = url.port or (int(url.path.split(':')[1]) if ':' in url.path else 8888)
                except:
                    port = 8888

                proxy = {
                    "scheme": scheme,
                    "hostname": hostname,
                    "port": port
                }

                if url.username:
                    proxy["username"] = url.username
                if url.password:
                    proxy["password"] = url.password

                logger.info(f"Using parsed proxy: {scheme}://{hostname}:{port}")
            except Exception as e:
                logger.warning(f"Failed to parse proxy string '{settings.telegram_proxy}': {e}. Using as is.")
                proxy = settings.telegram_proxy

        self.client = Client(
            name=session_name,
            api_id=settings.telegram_api_id,
            api_hash=settings.telegram_api_hash,
            in_memory=False,
            no_updates=True,
            proxy=proxy
        )

        # Check if session exists and is authorized
        session_file = f"{session_name}.session"
        session_exists = os.path.exists(session_file)

        if session_exists:
            try:
                # Try to start with existing session
                await self.client.connect()
                if self.client.is_connected:
                    try:
                        # Try to get me to check if authorized
                        me = await self.client.get_me()
                        if me:
                            self.is_ready = True
                            self.is_authorized = True
                            self.auth_state = "CONNECTED"
                            logger.info(f"Client started with existing session for tenant {self.tenant_id}")
                            # Setup deep link handler for /start command
                            await self._setup_deep_link_handler()
                            return
                        else:
                            logger.warning(f"Session exists but not authorized for {self.tenant_id}")
                            await self.client.disconnect()
                    except Exception as e:
                        logger.warning(f"Session exists but not authorized for {self.tenant_id}: {e}")
                        await self.client.disconnect()
                else:
                    logger.warning(f"Session exists but not connected for {self.tenant_id}")
            except Exception as e:
                logger.warning(f"Failed to start with existing session for {self.tenant_id}: {e}")

        # New session needed - wait for phone number
        logger.info(f"New session needed for tenant {self.tenant_id}")
        self.is_ready = False
        self.is_authorized = False
        self.auth_state = "DISCONNECTED"
        
    async def send_code(self, phone_number: str):
        """Send code to phone number"""
        if not self.client:
            await self.start()

        self.phone_number = phone_number  # Save phone number for sign_in

        # Ensure client is connected before sending code
        try:
            if not self.client.is_connected:
                await self.client.connect()
                logger.info(f"Client connected for tenant {self.tenant_id}")
            else:
                logger.info(f"Client already connected for tenant {self.tenant_id}")
        except Exception as e:
            logger.error(f"Failed to connect client: {e}")
            raise HTTPException(status_code=500, detail="Failed to connect to Telegram")

        try:
            # Send code to phone
            sent_code = await self.client.send_code(phone_number)
            self.phone_code_hash = sent_code.phone_code_hash
            self.auth_state = "WAITING_CODE"
            self.is_ready = True
            logger.info(f"Telegram send_code response: type={sent_code.type}, timeout={sent_code.timeout}")
            logger.info(f"Code sent to {phone_number} for tenant {self.tenant_id}")
            return {"status": "code_sent"}
        except Exception as e:
            logger.error(f"Failed to send code: {e}")
            raise HTTPException(status_code=500, detail=str(e))
        
    async def sign_in(self, code: str):
        """Sign in with code"""
        if not self.client or not self.phone_code_hash or not self.phone_number:
            raise HTTPException(status_code=400, detail="No code request pending")

        try:
            await self.client.sign_in(self.phone_number, self.phone_code_hash, code)
            self.is_ready = True
            self.is_authorized = True
            self.auth_state = "CONNECTED"
            logger.info(f"Client signed in for tenant {self.tenant_id}")
            
            # Setup deep link handler after successful authorization
            await self._setup_deep_link_handler()
            
            return {"status": "connected"}
        except Exception as e:
            logger.error(f"Failed to sign in: {e}")
            if "SESSION_PASSWORD_NEEDED" in str(e) or "SessionPasswordNeeded" in str(e):
                self.auth_state = "WAITING_PASSWORD"
                raise HTTPException(status_code=400, detail="PASSWORD_NEEDED")
            raise HTTPException(status_code=400, detail="Invalid code")
        
    async def check_password(self, password: str):
        """Check 2FA password"""
        if not self.client:
            raise HTTPException(status_code=400, detail="Client not ready")
            
        try:
            await self.client.check_password(password)
            self.is_ready = True
            self.is_authorized = True
            self.auth_state = "CONNECTED"
            logger.info(f"2FA password checked for tenant {self.tenant_id}")
            
            # Setup deep link handler after successful authorization
            await self._setup_deep_link_handler()
            
            return {"status": "connected"}
        except Exception as e:
            logger.error(f"Failed to check password: {e}")
            raise HTTPException(status_code=400, detail="Invalid password")
        
    async def stop(self):
        if self.client:
            try:
                if self.client.is_connected:
                    await self.client.disconnect()
                await self.client.stop()
            except Exception as e:
                logger.warning(f"Failed to stop client: {e}")
            self.client = None
        self.is_ready = False
        self.is_authorized = False
        self.auth_state = "DISCONNECTED"
             
    async def _setup_deep_link_handler(self):
        """Setup handler for /start command with deep link parameters."""
        if not self.client:
            return
            
        from pyrogram import filters
        
        @self.client.on_message(filters.command("start") & filters.private)
        async def handle_start(client, message):
            """Handle /start command with deep link parameter."""
            if not message.text or len(message.text.split()) < 2:
                return
            
            param = message.text.split(maxsplit=1)[1]
            chat_id = message.from_user.id
            
            logger.info(f"Deep link received: {param} from chat_id {chat_id}")
            
            # Parse parameter: contact_{id}, staff_{id}, user_{id}
            try:
                if param.startswith("contact_") or param.startswith("staff_") or param.startswith("user_"):
                    parts = param.split("_", 1)
                    if len(parts) == 2:
                        entity_type, entity_id = parts
                        await self._bind_telegram_id(entity_type, entity_id, chat_id)
                        await message.reply_text("✅ Аккаунт успешно привязан к CRM!")
                    else:
                        await message.reply_text("❌ Неверный формат ссылки")
                else:
                    await message.reply_text("ℹ️ Используйте специальную ссылку из CRM для привязки аккаунта")
            except Exception as e:
                logger.error(f"Failed to handle deep link: {e}")
                await message.reply_text("❌ Ошибка при привязке аккаунта")
    
    async def _bind_telegram_id(self, entity_type: str, entity_id: str, chat_id: int):
        """Send bind request to backend."""
        try:
            await http_client.post(
                f"{settings.backend_url}/api/admin/ai/internal/telegram/bind",
                json={"type": entity_type, "id": entity_id, "telegram_id": chat_id},
                headers={"X-Internal-Secret": settings.internal_secret},
                timeout=10.0
            )
            logger.info(f"Bound telegram_id {chat_id} to {entity_type} {entity_id} for tenant {self.tenant_id}")
        except Exception as e:
            logger.error(f"Failed to bind telegram_id: {e}")
             
    async def send_message(self, phone: str, name: str, text: str):
        if not self.client or not self.is_ready:
            raise HTTPException(status_code=400, detail="Client not connected")
            
        try:
            # Import contact using the proper type
            logger.info(f"Importing contact: {phone} ({name})")
            contacts = await self.client.import_contacts([
                InputPhoneContact(phone=phone, first_name=name or "Клиент CRM")
            ])
            
            if not contacts or not hasattr(contacts, "users") or not contacts.users:
                logger.warning(f"Contact not found for phone {phone}")
                raise HTTPException(status_code=404, detail="Contact not found")
                
            user = contacts.users[0]
            logger.info(f"Sending message to user_id {user.id}")

            # Send message
            await self.client.send_message(user.id, text)
            
            return {"status": "sent"}
            
        except Exception as e:
            error_trace = traceback.format_exc()
            logger.error(f"Failed to send message to {phone}: {e}\n{error_trace}")
            raise HTTPException(status_code=500, detail=str(e))

# Request/Response models
class SendMessageRequest(BaseModel):
    tenantId: str
    phone: str
    name: Optional[str] = None
    text: str

class PasswordRequest(BaseModel):
    tenantId: str
    password: str

class PhoneCodeRequest(BaseModel):
    tenantId: str
    phoneNumber: str

class SignInRequest(BaseModel):
    tenantId: str
    code: str

class StatusResponse(BaseModel):
    status: str
    qrCode: Optional[str] = None

async def get_session_lock(tenant_id: str) -> asyncio.Lock:
    async with session_locks_lock:
        if tenant_id not in session_locks:
            session_locks[tenant_id] = asyncio.Lock()
        return session_locks[tenant_id]

def generate_qr_base64(qr_link: str) -> str:
    qr = qrcode.QRCode(version=1, box_size=10, border=5)
    qr.add_data(qr_link)
    qr.make(fit=True)
    
    img = qr.make_image()
    buffered = BytesIO()
    img.save(buffered, format="PNG")
    return base64.b64encode(buffered.getvalue()).decode()

async def sync_status_with_backend(tenant_id: str, status: str, qr_link: Optional[str] = None):
    if is_stopping:
        return
        
    try:
        data = {
            "tenantId": tenant_id,
            "status": status
        }
        if qr_link:
            data["qrCode"] = qr_link
            
        response = await http_client.post(
            f"{settings.backend_url}/api/admin/telegram/internal/sync",
            json=data,
            headers={"X-Internal-Secret": settings.internal_secret},
            timeout=5.0
        )
        logger.info(f"Synced status {status} for tenant {tenant_id}")
    except Exception as e:
        logger.warning(f"Sync failed for {tenant_id}: {e}")

async def warmup_clients():
    """Auto-warmup clients on startup"""
    try:
        sessions_dir = settings.sessions_path
        if os.path.exists(sessions_dir) and os.path.isdir(sessions_dir):
            folders = [f for f in os.listdir(sessions_dir) if os.path.isdir(os.path.join(sessions_dir, f))]
            logger.info(f"Starting auto-warmup for {len(folders)} telegram clients...")
            
            for folder in folders:
                tenant_id = folder
                logger.info(f"Waking up client for tenant: {tenant_id}")
                asyncio.create_task(get_client(tenant_id))
    except Exception as e:
        logger.error(f"Failed to warmup telegram clients: {e}")

@app.get("/health")
async def health():
    return {"status": "ok"}

@app.post("/api/telegram/send-by-phone")
async def send_message_endpoint(
    request: SendMessageRequest,
    x_internal_secret: str = Header(...)
):
    if x_internal_secret != settings.internal_secret:
        raise HTTPException(status_code=401, detail="Invalid secret")
        
    tenant_id = request.tenantId
    
    client_wrapper = await get_client(tenant_id)
    if not client_wrapper:
        raise HTTPException(status_code=400, detail="OFFLINE")
        
    result = await client_wrapper.send_message(request.phone, request.name, request.text)
    return result

@app.get("/api/telegram/qr")
async def get_qr(
    tenantId: str,
    x_internal_secret: str = Header(...)
):
    if x_internal_secret != settings.internal_secret:
        raise HTTPException(status_code=401, detail="Invalid secret")

    client_wrapper = active_clients.get(tenantId)
    if client_wrapper:
        return {
            "status": client_wrapper.auth_state,
            "qrCode": pending_qr_links.get(tenantId),
            "link": pending_qr_links.get(tenantId)
        }

    return {
        "status": "DISCONNECTED",
        "qrCode": None,
        "link": None
    }

@app.get("/api/telegram/status")
async def get_status(
    tenantId: str,
    x_internal_secret: str = Header(...)
):
    if x_internal_secret != settings.internal_secret:
        raise HTTPException(status_code=401, detail="Invalid secret")

    client_wrapper = active_clients.get(tenantId)
    if client_wrapper:
        return {
            "status": client_wrapper.auth_state
        }

    return {
        "status": "DISCONNECTED"
    }

@app.delete("/api/telegram/session")
async def disconnect(
    tenantId: str,
    x_internal_secret: str = Header(...)
):
    if x_internal_secret != settings.internal_secret:
        raise HTTPException(status_code=401, detail="Invalid secret")
        
    await force_disconnect(tenantId)
    return {"status": "ok"}

@app.post("/api/telegram/connect")
async def connect(
    tenantId: str,
    x_internal_secret: str = Header(...)
):
    logger.info(f"Received connect request for tenant: {tenantId}")
    if x_internal_secret != settings.internal_secret:
        logger.warning("Invalid secret in connect request")
        raise HTTPException(status_code=401, detail="Invalid secret")
        
    logger.info(f"Connect request processed for tenant: {tenantId}")
    return {"status": "ok"}

@app.post("/api/telegram/send-code")
async def send_code(
    request: PhoneCodeRequest,
    x_internal_secret: str = Header(...)
):
    if x_internal_secret != settings.internal_secret:
        raise HTTPException(status_code=401, detail="Invalid secret")
        
    tenant_id = request.tenantId
    client_wrapper = await get_client(tenant_id)
    
    result = await client_wrapper.send_code(request.phoneNumber)
    await sync_status_with_backend(tenant_id, "WAITING_CODE", None)
    return result

@app.post("/api/telegram/sign-in")
async def sign_in(
    request: SignInRequest,
    x_internal_secret: str = Header(...)
):
    if x_internal_secret != settings.internal_secret:
        raise HTTPException(status_code=401, detail="Invalid secret")
        
    tenant_id = request.tenantId
    client_wrapper = active_clients.get(tenant_id)
    
    if not client_wrapper:
        raise HTTPException(status_code=400, detail="Client not found")
        
    try:
        result = await client_wrapper.sign_in(request.code)
        await sync_status_with_backend(tenant_id, "CONNECTED", None)
        return result
    except HTTPException as e:
        if e.detail == "PASSWORD_NEEDED":
            await sync_status_with_backend(tenant_id, "WAITING_PASSWORD", None)
        elif e.detail == "Invalid code":
            await sync_status_with_backend(tenant_id, "CODE_ERROR", None)
        raise

@app.post("/api/telegram/password")
async def check_password(
    request: PasswordRequest,
    x_internal_secret: str = Header(...)
):
    if x_internal_secret != settings.internal_secret:
        raise HTTPException(status_code=401, detail="Invalid secret")
        
    tenant_id = request.tenantId
    client_wrapper = active_clients.get(tenant_id)
    
    if not client_wrapper:
        raise HTTPException(status_code=400, detail="Client not found")
        
    try:
        result = await client_wrapper.check_password(request.password)
        await sync_status_with_backend(tenant_id, "CONNECTED", None)
        return result
    except HTTPException as e:
        if e.detail == "Invalid password":
            await sync_status_with_backend(tenant_id, "PASSWORD_ERROR", None)
        raise

@app.post("/api/telegram/cancel-qr")
async def cancel_qr_generation(
    tenantId: str,
    x_internal_secret: str = Header(...)
):
    if x_internal_secret != settings.internal_secret:
        raise HTTPException(status_code=401, detail="Invalid secret")
        
    lock = await get_session_lock(tenantId)
    async with lock:
        pending_qr_links.pop(tenantId, None)
        last_synced_qr_link.pop(tenantId, None)
        await sync_status_with_backend(tenantId, "DISCONNECTED", None)
        
        client_wrapper = active_clients.pop(tenantId, None)
        if client_wrapper:
            try:
                await client_wrapper.stop()
            except Exception as e:
                logger.warning(f"Failed to close client during QR cancellation: {e}")
                
        logger.info(f"QR generation cancelled for tenant {tenantId}")
        
    return {"status": "ok"}

async def get_client(tenant_id: str) -> Optional[TelegramClientWrapper]:
    lock = await get_session_lock(tenant_id)
    async with lock:
        if tenant_id in tenants_to_cleanup:
            return None
            
        if tenant_id in active_clients:
            return active_clients[tenant_id]
            
        client_wrapper = await create_new_client_instance(tenant_id)
        active_clients[tenant_id] = client_wrapper
        return client_wrapper

async def create_new_client_instance(tenant_id: str) -> TelegramClientWrapper:
    client_wrapper = TelegramClientWrapper(tenant_id)
    
    # Start the client (non-blocking)
    try:
        await client_wrapper.start()
        
        # Check if already authorized
        if client_wrapper.is_ready:
            await sync_status_with_backend(tenant_id, "CONNECTED", None)
        else:
            # Need to authorize
            await sync_status_with_backend(tenant_id, "DISCONNECTED", None)
    except Exception as e:
        logger.error(f"Failed to create client for {tenant_id}: {e}")
        await sync_status_with_backend(tenant_id, "DISCONNECTED", None)
        
    return client_wrapper

async def force_disconnect(tenant_id: str):
    lock = await get_session_lock(tenant_id)
    async with lock:
        tenants_to_cleanup.add(tenant_id)
        pending_qr_links.pop(tenant_id, None)
        last_synced_qr_link.pop(tenant_id, None)

        client_wrapper = active_clients.pop(tenant_id, None)
        if client_wrapper:
            try:
                await client_wrapper.stop()
            except Exception as e:
                logger.error(f"Failed to stop client: {e}")

        # Wait for database lock to be released
        await asyncio.sleep(0.5)

        # Cleanup files
        session_path = os.path.join(settings.sessions_path, tenant_id)
        if os.path.exists(session_path):
            shutil.rmtree(session_path)
            logger.info(f"Files cleared for {tenant_id}")
            
        await sync_status_with_backend(tenant_id, "DISCONNECTED", None)
        tenants_to_cleanup.discard(tenant_id)

async def initiate_reconnect(tenant_id: str):
    lock = await get_session_lock(tenant_id)
    async with lock:
        await force_disconnect(tenant_id)
        await get_client(tenant_id)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8081)
