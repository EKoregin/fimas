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
import ru.korevg.fimas.dto.port.PortCreateRequest;
import ru.korevg.fimas.dto.port.PortResponse;
import ru.korevg.fimas.dto.port.PortUpdateRequest;
import ru.korevg.fimas.service.PortService;

@RestController
@RequestMapping("/api/v1/ports")
@RequiredArgsConstructor
@Tag(name = "Ports", description = "Управление портами и протоколами")
public class PortController {

    private final PortService portService;

    @PostMapping
    @Operation(summary = "Создать новый порт")
    public ResponseEntity<PortResponse> create(@Valid @RequestBody PortCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(portService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить порт по ID")
    public ResponseEntity<PortResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(portService.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить порт")
    public ResponseEntity<PortResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody PortUpdateRequest request) {
        return ResponseEntity.ok(portService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить порт")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        portService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Получить все порты с пагинацией")
    public ResponseEntity<Page<PortResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(portService.findAll(pageable));
    }
}