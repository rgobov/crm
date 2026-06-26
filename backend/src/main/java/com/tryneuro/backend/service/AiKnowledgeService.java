package com.tryneuro.backend.service;

import com.tryneuro.backend.model.AiKnowledge;
import com.tryneuro.backend.repository.AiKnowledgeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiKnowledgeService {

    private final AiKnowledgeRepository repository;

    public List<AiKnowledge> getAll(String tenantId, String category) {
        if (category != null && !category.isEmpty()) {
            return repository.findByTenantIdAndCategoryOrderByCreatedAtDesc(tenantId, category);
        }
        return repository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional
    public AiKnowledge create(String tenantId, String question, String answer, String category) {
        AiKnowledge entry = new AiKnowledge();
        entry.setTenantId(tenantId);
        entry.setQuestion(question);
        entry.setAnswer(answer);
        entry.setCategory(category != null && !category.isEmpty() ? category : "FAQ");
        entry.setCreatedAt(LocalDateTime.now());
        entry.setUpdatedAt(LocalDateTime.now());
        return repository.save(entry);
    }

    @Transactional
    public void delete(String id, String tenantId) {
        AiKnowledge entry = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Knowledge entry not found"));
        if (!entry.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Knowledge entry does not belong to this tenant");
        }
        repository.delete(entry);
    }

    public List<AiKnowledge> search(String tenantId, String query) {
        return repository.findByTenantIdAndQuestionContainingIgnoreCase(tenantId, query);
    }
}
