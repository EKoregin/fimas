package ru.korevg.fimas.dto.service;

import jakarta.validation.constraints.Size;
import java.util.Set;

public record ServiceUpdateRequest(

        @Size(min = 1, max = 100)
        String name,

        @Size(max = 500)
        String description,

        Set<Long> portIds
) {}
