package com.tryneuro.backend.service;

import com.tryneuro.backend.repository.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public com.tryneuro.backend.model.Service addService(com.tryneuro.backend.model.Service service, String tenantId) {
        service.setTenantId(tenantId);
        return serviceRepository.save(service);
    }

    public void deleteService(String id) {
        serviceRepository.deleteById(id);
    }
}
