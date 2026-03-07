package ru.korevg.fimas.dto.firewall;

public record FirewallResponse(
        Long id,
        String name,
        String description,
        Long modelId,
        String mgmtIpAddress,
        String modelName,
        String vendorName
) {}
