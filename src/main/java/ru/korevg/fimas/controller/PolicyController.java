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
import ru.korevg.fimas.dto.policy.PolicyCreateRequest;
import ru.korevg.fimas.dto.policy.PolicyResponse;
import ru.korevg.fimas.dto.policy.PolicyUpdateRequest;
import ru.korevg.fimas.service.PolicyService;

@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
@Tag(name = "Policies", description = "Управление политиками безопасности")
public class PolicyController {

    private final PolicyService policyService;

    @PostMapping
    @Operation(summary = "Создать новую политику")
    public ResponseEntity<PolicyResponse> create(@Valid @RequestBody PolicyCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(policyService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить политику по ID")
    public ResponseEntity<PolicyResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(policyService.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить политику")
    public ResponseEntity<PolicyResponse> update(@PathVariable Long id,
                                                 @Valid @RequestBody PolicyUpdateRequest request) {
        return ResponseEntity.ok(policyService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить политику")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        policyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Получить список всех политик с пагинацией")
    public ResponseEntity<Page<PolicyResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(policyService.findAll(pageable));
    }

    @GetMapping("/firewall/{firewallId}")
    @Operation(summary = "Получить все политики конкретного firewall")
    public ResponseEntity<Page<PolicyResponse>> getByFirewall(@PathVariable Long firewallId,
                                                              Pageable pageable) {
        return ResponseEntity.ok(policyService.findByFirewallId(firewallId, pageable));
    }
}