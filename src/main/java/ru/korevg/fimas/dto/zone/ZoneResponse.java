package ru.korevg.fimas.dto.zone;

public record ZoneResponse(
        Long id,
        String name,
        String description,
        Integer priority
) {}