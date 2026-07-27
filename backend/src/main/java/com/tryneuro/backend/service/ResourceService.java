package com.tryneuro.backend.service;

import com.tryneuro.backend.model.Resource;
import com.tryneuro.backend.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final ImageCompressionService imageCompressionService;

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

    @Transactional
    public Resource updateResourcePhoto(String id, MultipartFile file, String tenantId) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ресурс не найден"));
        if (!resource.getTenantId().equals(tenantId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Доступ запрещен");
        }
        try {
            byte[] photoBytes = imageCompressionService.compress(file);
            resource.setPhotoData(photoBytes);
            resource.setUpdatedAt(LocalDateTime.now());
            return resourceRepository.save(resource);
        } catch (Exception e) {
            log.warn("Ошибка при обработке фото ресурса: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ошибка при обработке фото: " + e.getMessage());
        }
    }

    @Transactional
    public Resource deleteResourcePhoto(String id, String tenantId) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ресурс не найден"));
        if (!resource.getTenantId().equals(tenantId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Доступ запрещен");
        }
        resource.setPhotoData(null);
        resource.setUpdatedAt(LocalDateTime.now());
        return resourceRepository.save(resource);
    }
}