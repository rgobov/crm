/** TryNeuro Provider for OpenClaw
 *
 * Per-user OpenRouter API key + model from CRM PostgreSQL.
 * Uses resolveSyntheticAuth + wrapStreamFn to inject per-user API key.
 */
import { definePluginEntry } from "openclaw/plugin-sdk/plugin-entry";
import pg from "pg";

const { Pool } = pg;

const DB_URL = process.env.DATABASE_URL || "postgresql://postgres:postgres@tryneuro_database:5432/tryneuro_db";
const CACHE_TTL = 300000; // 5 minutes

interface UserConfig {
  llm_model: string;
  api_key: string;
}

const configCache = new Map<string, { config: UserConfig; ts: number }>();

const pool = new Pool({ connectionString: DB_URL });

async function getUserConfig(telegramId: number): Promise<UserConfig | null> {
  const key = String(telegramId);
  const now = Date.now();
  
  const cached = configCache.get(key);
  if (cached && now - cached.ts < CACHE_TTL) {
    return cached.config;
  }
  
  try {
    const result = await pool.query(
      'SELECT uac.llm_model, uac.api_key FROM "users" u JOIN user_ai_config uac ON u.id = uac.user_id WHERE u.telegram_id = $1 AND uac.api_key IS NOT NULL AND uac.api_key != \'\'',
      [telegramId]
    );
    
    if (result.rows.length > 0) {
      const config = {
        llm_model: result.rows[0].llm_model,
        api_key: result.rows[0].api_key
      };
      configCache.set(key, { config, ts: now });
      console.log(`[tryneuro-provider] loaded tg=${telegramId} model=${config.llm_model}`);
      return config;
    }
    return null;
  } catch (e) {
    console.error(`[tryneuro-provider] DB error for tg=${telegramId}:`, e);
    return null;
  }
}

export default definePluginEntry({
  id: "tryneuro-provider",
  name: "TryNeuro Provider",
  description: "Per-user OpenRouter model + API key from CRM",

  register(api) {
    api.registerProvider({
      id: "tryneuro",
      label: "TryNeuro OpenRouter",
      docsPath: "/providers/tryneuro",
      envVars: ["DATABASE_URL"],
      
      catalog: {
        order: "simple",
        run: async () => ({
          provider: {
            api: "openai-completions",
            baseUrl: "https://openrouter.ai/api/v1"
          }
        })
      },

      resolveDynamicModel: (ctx) => ({
        id: "openrouter/auto",
        name: "TryNeuro Dynamic",
        provider: "tryneuro",
        api: "openai-completions",
        baseUrl: "https://openrouter.ai/api/v1",
        reasoning: false,
        input: ["text"],
        cost: { input: 0, output: 0, cacheRead: 0, cacheWrite: 0 },
        contextWindow: 128000,
        maxTokens: 8192
      }),

      resolveSyntheticAuth: async (ctx) => {
        const telegramId = ctx.ctx?.channelContext?.sender?.id;
        if (telegramId) {
          const id = telegramId.replace("tg:", "").replace("+", "");
          const tgId = parseInt(id, 10);
          const config = await getUserConfig(tgId);
          
          if (config?.api_key) {
            return { kind: "static", source: "user-config", apiKey: config.api_key };
          }
        }
        
        const sharedKey = process.env.OPENROUTER_API_KEY;
        if (sharedKey) {
          return { kind: "static", source: "env", apiKey: sharedKey };
        }
        
        return null;
      },

      wrapStreamFn: async (ctx, params) => {
        const telegramId = ctx.ctx?.channelContext?.sender?.id;
        if (telegramId) {
          const id = telegramId.replace("tg:", "").replace("+", "");
          const tgId = parseInt(id, 10);
          const config = await getUserConfig(tgId);
          
          if (config?.llm_model) {
            params.model = config.llm_model;
            ctx.logger?.debug(`[tryneuro-provider] Using user model: ${config.llm_model}`);
          }
        }
        
        return ctx.streamFn(params);
      }
    });
  }
});