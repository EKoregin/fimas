package ru.korevg.fimas.dto.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record ServiceCreateRequest(

        @NotBlank(message = "Имя сервиса обязательно")
        @Size(min = 1, max = 100)
        String name,

        @Size(max = 500)
        String description,

        Set<Long> portIds
) {}