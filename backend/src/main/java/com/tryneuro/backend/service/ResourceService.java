package com.tryneuro.backend.service;

import com.tryneuro.backend.model.Resource;
import com.tryneuro.backend.repository.ResourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResourceService {
    private final ResourceRepository resourceRepository;

    @Autowired
    public ResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    public List<Resource> getAllResources(String tenantId) {
        return resourceRepository.findByTenantId(tenantId);
    }

    public Resource addResource(Resource resource, String tenantId) {
        resource.setTenantId(tenantId);
        return resourceRepository.save(resource);
    }

    public void deleteResource(String id) {
        resourceRepository.deleteById(id);
    }
}
