package com.tryneuro.backend.repository;

import com.tryneuro.backend.model.AiKnowledge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiKnowledgeRepository extends JpaRepository<AiKnowledge, String> {

    List<AiKnowledge> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    List<AiKnowledge> findByTenantIdAndCategoryOrderByCreatedAtDesc(String tenantId, String category);

    List<AiKnowledge> findByTenantIdAndQuestionContainingIgnoreCase(String tenantId, String query);
}
