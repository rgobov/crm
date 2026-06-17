package com.tryneuro.backend.controller.flutter;

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
    public List<Service> getAllServices(@RequestAttribute("tenantId") String tenantId) {
        return appServiceService.getAllServices(tenantId);
    }

    @PostMapping
    public Service addService(@RequestBody Service service, @RequestAttribute("tenantId") String tenantId) {
        return appServiceService.addService(service, tenantId);
    }

    @PutMapping("/{id}")
    public Service updateService(@PathVariable String id, @RequestBody Service service, @RequestAttribute("tenantId") String tenantId) {
        return appServiceService.updateService(id, service, tenantId);
    }

    @DeleteMapping("/{id}")
    public void deleteService(@PathVariable String id) {
        appServiceService.deleteService(id);
    }
}
