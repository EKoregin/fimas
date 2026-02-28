package ru.korevg.fimas.dto.service;

import java.util.Set;

public record ServiceResponse(
        Long id,
        String name,
        String description,
        Set<PortShortResponse> ports
) {
    public record PortShortResponse(Long id, String protocol, String srcPort, String dstPort) {}
}
