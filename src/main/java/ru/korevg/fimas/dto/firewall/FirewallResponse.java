package ru.korevg.fimas.dto.firewall;

public record FirewallResponse(
        Long id,
        String name,
        String description,
        Long modelId,
        String modelName,
        String vendorName
) {}
