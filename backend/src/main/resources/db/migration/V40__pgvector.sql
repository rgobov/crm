CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE ai_knowledge_chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES users(id),
    knowledge_id UUID REFERENCES ai_knowledge(id) ON DELETE CASCADE,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    embedding vector(1536),
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_chunks_tenant ON ai_knowledge_chunks(tenant_id);
CREATE INDEX idx_chunks_embedding ON ai_knowledge_chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
