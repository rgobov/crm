package com.tryneuro.backend.controller;

import com.tryneuro.backend.model.Service;
import com.tryneuro.backend.service.AppServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
public class AppServiceController {
    private final AppServiceService appServiceService;

    @Autowired
    public AppServiceController(AppServiceService appServiceService) {
        this.appServiceService = appServiceService;
    }

    @GetMapping
    public List<Service> getAllServices(@RequestHeader("X-Tenant-ID") String tenantId) {
        return appServiceService.getAllServices(tenantId);
    }

    @PostMapping
    public Service createService(@RequestHeader("X-Tenant-ID") String tenantId, @RequestBody Service service) {
        return appServiceService.addService(service, tenantId);
    }

    @DeleteMapping("/{id}")
    public void deleteService(@PathVariable String id) {
        appServiceService.deleteService(id);
    }
}
