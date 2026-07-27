package com.tryneuro.backend.service;

import com.tryneuro.backend.repository.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AppServiceService {
    private final ServiceRepository serviceRepository;

    @Autowired
    public AppServiceService(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    public List<com.tryneuro.backend.model.Service> getAllServices(String tenantId) {
        return serviceRepository.findByTenantId(tenantId);
    }

    /**
     * Услуги для конкретной ниши: niche IS NULL OR niche = ?
     * NULL-услуги — общие, видны в филиалах любой ниши.
     */
    public List<com.tryneuro.backend.model.Service> getServicesByNiche(String tenantId, String niche) {
        if (niche == null || niche.isEmpty()) {
            return serviceRepository.findByTenantId(tenantId);
        }
        List<com.tryneuro.backend.model.Service> result = new java.util.ArrayList<>();
        result.addAll(serviceRepository.findByTenantIdAndNicheIsNull(tenantId));
        result.addAll(serviceRepository.findByTenantIdAndNiche(tenantId, niche));
        return result;
    }

    public com.tryneuro.backend.model.Service addService(com.tryneuro.backend.model.Service service, String tenantId) {
        service.setTenantId(tenantId);
        return serviceRepository.save(service);
    }

    public com.tryneuro.backend.model.Service updateService(String id, com.tryneuro.backend.model.Service details, String tenantId) {
        com.tryneuro.backend.model.Service existing = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found: " + id));
        if (!existing.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Access denied");
        }
        existing.setName(details.getName());
        existing.setDurationInMinutes(details.getDurationInMinutes());
        existing.setPriceMin(details.getPriceMin());
        existing.setPriceMax(details.getPriceMax());
        existing.setNiche(details.getNiche());
        return serviceRepository.save(existing);
    }

    public void deleteService(String id) {
        serviceRepository.deleteById(id);
    }

    public Optional<com.tryneuro.backend.model.Service> getServiceById(String id) {
        return serviceRepository.findById(id);
    }
}
