package com.tryneuro.backend.service;

import com.tryneuro.backend.model.Resource;
import com.tryneuro.backend.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourceService {
    
    private final ResourceRepository resourceRepository;

    public List<Resource> getAllResources(String tenantId) {
        return resourceRepository.findByTenantId(tenantId);
    }

    public List<Resource> getResources(String tenantId, String branchId) {
        if (branchId == null || branchId.isEmpty() || "null".equals(branchId)) {
            return resourceRepository.findByTenantId(tenantId);
        }
        return resourceRepository.findByTenantIdAndBranchId(tenantId, branchId);
    }

    /**
     * Безопасное сохранение нового ресурса. 
     * tenantId берется из контекста безопасности, а не из тела запроса.
     */
    @Transactional
    public Resource addResource(Resource resource, String tenantId) {
        resource.setTenantId(tenantId);
        return resourceRepository.save(resource);
    }

    /**
     * Безопасное обновление ресурса.
     */
    @Transactional
    public Resource updateResource(String id, Resource details, String tenantId) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Resource not found"));
        
        // Гарантируем, что нельзя обновить чужой ресурс
        if (!resource.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Access denied: You do not own this resource");
        }

        resource.setName(details.getName());
        resource.setDescription(details.getDescription());
        resource.setBranchId(details.getBranchId());
        
        return resourceRepository.save(resource);
    }

    @Transactional
    public void deleteResource(String id) {
        resourceRepository.deleteById(id);
    }
}
