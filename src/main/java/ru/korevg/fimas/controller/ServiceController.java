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
import ru.korevg.fimas.dto.service.ServiceCreateRequest;
import ru.korevg.fimas.dto.service.ServiceResponse;
import ru.korevg.fimas.dto.service.ServiceUpdateRequest;
import ru.korevg.fimas.service.ServiceService;

@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
@Tag(name = "Services", description = "Управление сервисами (группами портов)")
public class ServiceController {

    private final ServiceService serviceService;

    @PostMapping
    @Operation(summary = "Создать новый сервис")
    public ResponseEntity<ServiceResponse> create(@Valid @RequestBody ServiceCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить сервис по ID")
    public ResponseEntity<ServiceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(serviceService.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить сервис")
    public ResponseEntity<ServiceResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody ServiceUpdateRequest request) {
        return ResponseEntity.ok(serviceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить сервис")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        serviceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Получить все сервисы с пагинацией")
    public ResponseEntity<Page<ServiceResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(serviceService.findAll(pageable));
    }
}
