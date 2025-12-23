package com.tryneuro.backend.controller;

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
    public Resource createResource(@RequestAttribute("tenantId") String tenantId, @RequestBody Resource resource) {
        return resourceService.addResource(resource, tenantId);
    }

    @DeleteMapping("/{id}")
    public void deleteResource(@PathVariable String id) {
        resourceService.deleteResource(id);
    }
}
