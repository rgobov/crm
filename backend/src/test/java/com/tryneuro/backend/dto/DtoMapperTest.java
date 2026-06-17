package com.tryneuro.backend.dto;

import com.tryneuro.backend.model.Service;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DtoMapperTest {

    @Test
    void testServiceToDtoMapping() {
        Service service = new Service();
        service.setId("test-id");
        service.setName("Test Service");
        service.setDurationInMinutes(45);
        service.setPriceMin(1500);
        service.setPriceMax(2500);
        service.setTenantId("test-tenant");

        ServiceDto dto = DtoMapper.toDto(service);

        assertNotNull(dto);
        assertEquals("test-id", dto.getId());
        assertEquals("Test Service", dto.getName());
        assertEquals(45, dto.getDurationInMinutes());
        assertEquals(1500, dto.getPriceMin());
        assertEquals(2500, dto.getPriceMax());
    }

    @Test
    void testServiceToEntityMapping() {
        ServiceDto dto = ServiceDto.builder()
                .id("test-id")
                .name("Test Service")
                .durationInMinutes(45)
                .priceMin(1000)
                .priceMax(null)
                .build();

        Service service = DtoMapper.toEntity(dto, "test-tenant");

        assertNotNull(service);
        assertEquals("test-id", service.getId());
        assertEquals("Test Service", service.getName());
        assertEquals(45, service.getDurationInMinutes());
        assertEquals(1000, service.getPriceMin());
        assertNull(service.getPriceMax());
        assertEquals("test-tenant", service.getTenantId());
    }

    @Test
    void testServiceMappingWithNulls() {
        assertNull(DtoMapper.toDto((Service) null));
        assertNull(DtoMapper.toEntity((ServiceDto) null, "tenant"));
    }
}
