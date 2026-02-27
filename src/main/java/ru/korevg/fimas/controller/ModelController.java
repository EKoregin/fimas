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
import ru.korevg.fimas.dto.model.ModelCreateRequest;
import ru.korevg.fimas.dto.model.ModelResponse;
import ru.korevg.fimas.dto.model.ModelUpdateRequest;
import ru.korevg.fimas.service.ModelService;

@RestController
@RequestMapping("/api/v1/models")
@RequiredArgsConstructor
@Tag(name = "Models", description = "Управление моделями оборудования")
public class ModelController {

    private final ModelService modelService;

    @PostMapping
    @Operation(summary = "Создать новую модель")
    public ResponseEntity<ModelResponse> create(@Valid @RequestBody ModelCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(modelService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить модель по ID")
    public ResponseEntity<ModelResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(modelService.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить модель")
    public ResponseEntity<ModelResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody ModelUpdateRequest request) {
        return ResponseEntity.ok(modelService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить модель")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        modelService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Получить список всех моделей с пагинацией")
    public ResponseEntity<Page<ModelResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(modelService.findAll(pageable));
    }
}
