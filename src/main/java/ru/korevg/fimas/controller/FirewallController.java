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
import ru.korevg.fimas.dto.firewall.FirewallCreateRequest;
import ru.korevg.fimas.dto.firewall.FirewallResponse;
import ru.korevg.fimas.dto.firewall.FirewallUpdateRequest;
import ru.korevg.fimas.service.FirewallService;

@RestController
@RequestMapping("/api/v1/firewalls")
@RequiredArgsConstructor
@Tag(name = "Firewalls", description = "Управление межсетевыми экранами")
public class FirewallController {

    private final FirewallService firewallService;

    @PostMapping
    @Operation(summary = "Создать новый firewall")
    public ResponseEntity<FirewallResponse> create(@Valid @RequestBody FirewallCreateRequest request) {
        FirewallResponse response = firewallService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить firewall по ID")
    public ResponseEntity<FirewallResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(firewallService.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить firewall")
    public ResponseEntity<FirewallResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody FirewallUpdateRequest request) {
        return ResponseEntity.ok(firewallService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить firewall")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        firewallService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Получить список всех firewalls с пагинацией")
    public ResponseEntity<Page<FirewallResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(firewallService.findAll(pageable));
    }
}