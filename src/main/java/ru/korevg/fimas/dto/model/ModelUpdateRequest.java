package ru.korevg.fimas.dto.model;

import jakarta.validation.constraints.Size;

public record ModelUpdateRequest(

        @Size(min = 1, max = 100, message = "Название от 1 до 100 символов")
        String name,

        Long vendorId
) {}