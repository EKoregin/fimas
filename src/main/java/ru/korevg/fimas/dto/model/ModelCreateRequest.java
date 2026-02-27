package ru.korevg.fimas.dto.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ModelCreateRequest(

        @NotBlank(message = "Название модели обязательно")
        @Size(min = 1, max = 100, message = "Название от 1 до 100 символов")
        String name,

        @NotNull(message = "ID вендора обязателен")
        Long vendorId
) {}
