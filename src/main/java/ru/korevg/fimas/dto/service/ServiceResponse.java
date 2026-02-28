package ru.korevg.fimas.dto.service;

import ru.korevg.fimas.dto.port.PortShortResponse;

import java.util.Set;

public record ServiceResponse(
        Long id,
        String name,
        String description,
        Set<PortShortResponse> ports
) {}
