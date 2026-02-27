package ru.korevg.fimas.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.korevg.fimas.dto.vendor.VendorCreateRequest;
import ru.korevg.fimas.dto.vendor.VendorResponse;
import ru.korevg.fimas.entity.Vendor;
import ru.korevg.fimas.exception.EntityExistsException;
import ru.korevg.fimas.repository.VendorRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class VendorServiceImplTest {

    @Autowired
    private VendorService vendorService;

    @Autowired
    private VendorRepository vendorRepository;

    @Test
    void create_shouldSaveNewVendor() {
        VendorCreateRequest request = new VendorCreateRequest("TestVendor");

        VendorResponse response = vendorService.create(request);

        assertNotNull(response.id());
        assertEquals("TestVendor", response.name());

        assertTrue(vendorRepository.existsByName("TestVendor"));
    }

    @Test
    void create_shouldThrowWhenNameExists() {
        vendorRepository.save(Vendor.builder().name("Duplicate").build());

        VendorCreateRequest request = new VendorCreateRequest("Duplicate");

        assertThrows(EntityExistsException.class, () -> vendorService.create(request));
    }
}