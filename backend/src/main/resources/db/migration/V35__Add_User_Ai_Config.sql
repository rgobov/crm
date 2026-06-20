CREATE TABLE user_ai_config (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    llm_provider VARCHAR DEFAULT 'openrouter',
    llm_model VARCHAR DEFAULT 'openrouter/auto',
    api_key TEXT,
    stt_provider VARCHAR DEFAULT 'vosk',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);