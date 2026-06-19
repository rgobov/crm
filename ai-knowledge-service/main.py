import os
import uuid
import json
import io
import wave
import logging
from fastapi import FastAPI, HTTPException, Header, UploadFile, File
from pydantic import BaseModel
from contextlib import asynccontextmanager
from database import init_db, query_db, execute_db
from pydub import AudioSegment
from vosk import Model, KaldiRecognizer

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

MODEL_PATH = os.getenv("VOSK_MODEL_PATH", "vosk-model-small-ru-0.22")
vosk_model = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    global vosk_model
    init_db()
    if os.path.isdir(MODEL_PATH):
        vosk_model = Model(MODEL_PATH)
        logger.info(f"Vosk model loaded from {MODEL_PATH}")
    else:
        logger.warning(f"Vosk model not found at {MODEL_PATH}, STT disabled")
    logger.info("ai-knowledge-service started")
    yield

app = FastAPI(title="AI Knowledge Service", lifespan=lifespan)

INTERNAL_SECRET = os.getenv("INTERNAL_SECRET", "try-neuro-internal-secret-2026")

def verify_secret(secret: str):
    if secret != INTERNAL_SECRET:
        raise HTTPException(401, "Invalid secret")

class KnowledgeEntry(BaseModel):
    question: str
    answer: str
    category: str = "FAQ"

class KnowledgeResponse(BaseModel):
    id: str
    question: str
    answer: str
    category: str

@app.post("/api/v1/knowledge/{tenant_id}", response_model=KnowledgeResponse)
def create_knowledge(tenant_id: str, entry: KnowledgeEntry, x_internal_secret: str = Header(...)):
    verify_secret(x_internal_secret)
    entry_id = str(uuid.uuid4())
    execute_db(
        "INSERT INTO knowledge_base (id, tenant_id, question, answer, category) VALUES (?, ?, ?, ?, ?)",
        (entry_id, tenant_id, entry.question, entry.answer, entry.category)
    )
    return KnowledgeResponse(id=entry_id, question=entry.question, answer=entry.answer, category=entry.category)

@app.get("/api/v1/knowledge/{tenant_id}", response_model=list[KnowledgeResponse])
def list_knowledge(tenant_id: str, category: str = None, x_internal_secret: str = Header(...)):
    verify_secret(x_internal_secret)
    if category:
        rows = query_db("SELECT * FROM knowledge_base WHERE tenant_id = ? AND category = ?", (tenant_id, category))
    else:
        rows = query_db("SELECT * FROM knowledge_base WHERE tenant_id = ?", (tenant_id,))
    return [KnowledgeResponse(**row) for row in rows]

@app.delete("/api/v1/knowledge/{entry_id}")
def delete_knowledge(entry_id: str, x_internal_secret: str = Header(...)):
    verify_secret(x_internal_secret)
    execute_db("DELETE FROM knowledge_base WHERE id = ?", (entry_id,))
    return {"status": "deleted"}

@app.post("/api/v1/knowledge/{tenant_id}/search")
def search_knowledge(tenant_id: str, body: dict, x_internal_secret: str = Header(...)):
    verify_secret(x_internal_secret)
    query = body.get("query", "").lower()
    rows = query_db("SELECT * FROM knowledge_base WHERE tenant_id = ?", (tenant_id,))
    results = []
    for row in rows:
        if query in row["question"].lower() or query in row["answer"].lower():
            results.append(row)
    return {"results": results}

class AiConfig(BaseModel):
    llm_provider: str = "yandex"
    llm_model: str = "yandexgpt"
    api_key: str = ""
    stt_provider: str = "vosk"

@app.get("/api/v1/config/{tenant_id}")
def get_config(tenant_id: str, x_internal_secret: str = Header(...)):
    verify_secret(x_internal_secret)
    rows = query_db("SELECT * FROM tenant_ai_config WHERE tenant_id = ?", (tenant_id,))
    if not rows:
        return {"tenant_id": tenant_id, "llm_provider": "yandex", "llm_model": "yandexgpt", "api_key": "", "stt_provider": "vosk"}
    return rows[0]

@app.put("/api/v1/config/{tenant_id}")
def upsert_config(tenant_id: str, config: AiConfig, x_internal_secret: str = Header(...)):
    verify_secret(x_internal_secret)
    execute_db("""
        INSERT INTO tenant_ai_config (tenant_id, llm_provider, llm_model, api_key, stt_provider, updated_at)
        VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
        ON CONFLICT(tenant_id) DO UPDATE SET
            llm_provider = excluded.llm_provider,
            llm_model = excluded.llm_model,
            api_key = excluded.api_key,
            stt_provider = excluded.stt_provider,
            updated_at = CURRENT_TIMESTAMP
    """, (tenant_id, config.llm_provider, config.llm_model, config.api_key, config.stt_provider))
    return {"status": "saved", "tenant_id": tenant_id}

@app.post("/api/v1/stt/{tenant_id}")
async def transcribe(tenant_id: str, file: UploadFile = File(...), x_internal_secret: str = Header(...)):
    verify_secret(x_internal_secret)

    if vosk_model is None:
        raise HTTPException(503, "Vosk model not loaded")

    contents = await file.read()

    try:
        audio = AudioSegment.from_file(io.BytesIO(contents))
    except Exception:
        raise HTTPException(400, "Unsupported audio format")

    audio = audio.set_frame_rate(16000).set_channels(1).set_sample_width(2)
    duration_ms = len(audio)

    raw_data = audio.raw_data
    wav_buf = io.BytesIO()
    with wave.open(wav_buf, "wb") as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(16000)
        wf.writeframes(raw_data)
    wav_buf.seek(0)

    rec = KaldiRecognizer(vosk_model, 16000)
    rec.SetWords(True)

    wav_file = wave.open(wav_buf, "rb")
    data = wav_file.readframes(wav_file.getnframes())
    if rec.AcceptWaveform(data):
        result = json.loads(rec.Result())
    else:
        result = json.loads(rec.FinalResult())

    text = result.get("text", "")
    confidence = None
    if "result" in result and result["result"]:
        confidence = sum(w["conf"] for w in result["result"]) / len(result["result"])

    log_id = str(uuid.uuid4())
    execute_db(
        "INSERT INTO stt_log (id, tenant_id, duration_ms, text, confidence) VALUES (?, ?, ?, ?, ?)",
        (log_id, tenant_id, duration_ms, text, confidence)
    )

    return {"text": text, "confidence": confidence, "duration_ms": duration_ms}

@app.get("/health")
def health():
    return {"status": "ok"}
