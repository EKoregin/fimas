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
import ru.korevg.fimas.dto.address.*;
import ru.korevg.fimas.service.AddressService;

@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
@Tag(name = "Addresses", description = "Управление адресами (Common и Dynamic)")
public class AddressController {

    private final AddressService addressService;

    @PostMapping("/common")
    @Operation(summary = "Создать глобальный адрес")
    public ResponseEntity<AddressResponse> createCommon(@Valid @RequestBody AddressCommonCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(addressService.createCommon(request));
    }

    @PostMapping("/dynamic")
    @Operation(summary = "Создать динамический адрес")
    public ResponseEntity<AddressResponse> createDynamic(@Valid @RequestBody AddressDynamicCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(addressService.createDynamic(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить адрес по ID")
    public ResponseEntity<AddressResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(addressService.findById(id));
    }

    @PutMapping("/common/{id}")
    @Operation(summary = "Обновить глобальный адрес")
    public ResponseEntity<AddressResponse> updateCommon(
            @PathVariable Long id,
            @Valid @RequestBody AddressCommonCreateRequest request) {

        return ResponseEntity.ok(addressService.updateCommon(id, request));
    }

    @PutMapping("/dynamic/{id}")
    @Operation(summary = "Обновить динамический адрес")
    public ResponseEntity<AddressResponse> updateDynamic(
            @PathVariable Long id,
            @Valid @RequestBody AddressDynamicCreateRequest request) {

        return ResponseEntity.ok(addressService.updateDynamic(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить адрес")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        addressService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Получить все адреса с пагинацией")
    public ResponseEntity<Page<AddressResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(addressService.findAll(pageable));
    }
}