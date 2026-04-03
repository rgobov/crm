package com.tryneuro.backend.controller.flutter;

import com.tryneuro.backend.model.Resource;
import com.tryneuro.backend.service.ResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceService resourceService;

    @Autowired
    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    public List<Resource> getAllResources(@RequestAttribute("tenantId") String tenantId) {
        return resourceService.getAllResources(tenantId);
    }

    @PostMapping
    public Resource addResource(@RequestBody Resource resource, @RequestAttribute("tenantId") String tenantId) {
        return resourceService.addResource(resource, tenantId);
    }

    @DeleteMapping("/{id}")
    public void deleteResource(@PathVariable String id) {
        resourceService.deleteResource(id);
    }
}
