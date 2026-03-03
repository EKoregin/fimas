package ru.korevg.fimas.dto.service;

import ru.korevg.fimas.dto.RecordWithName;

public record ServiceShortResponse(
        Long id,
        String name
) implements RecordWithName {}
