package ru.korevg.fimas.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.korevg.fimas.dto.vendor.VendorCreateRequest;
import ru.korevg.fimas.dto.vendor.VendorResponse;
import ru.korevg.fimas.dto.vendor.VendorUpdateRequest;
import ru.korevg.fimas.service.VendorService;

@RestController
@RequestMapping("/api/v1/vendors")
@RequiredArgsConstructor
@Tag(name = "Vendors", description = "Управление вендорами (производителями)")
public class VendorController {

    private final VendorService vendorService;

    @PostMapping
    @Operation(summary = "Создать нового вендора")
    public ResponseEntity<VendorResponse> create(@Valid @RequestBody VendorCreateRequest request) {
        VendorResponse response = vendorService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить вендора по ID")
    public ResponseEntity<VendorResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(vendorService.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить вендора")
    public ResponseEntity<VendorResponse> update(@PathVariable Long id,
                                                 @Valid @RequestBody VendorUpdateRequest request) {
        return ResponseEntity.ok(vendorService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить вендора")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        vendorService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Получить список всех вендоров с пагинацией")
    public ResponseEntity<Page<VendorResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(vendorService.findAll(pageable));
    }
}