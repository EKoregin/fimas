package ru.korevg.fimas.dto.vendor;

import jakarta.validation.constraints.Size;

public record VendorUpdateRequest(

        @Size(min = 1, max = 100, message = "Название должно быть от 1 до 100 символов")
        String name
) {}