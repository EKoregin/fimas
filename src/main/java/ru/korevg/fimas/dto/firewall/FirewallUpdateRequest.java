package ru.korevg.fimas.dto.firewall;

import jakarta.validation.constraints.Size;

public record FirewallUpdateRequest(

        @Size(min = 1, max = 100, message = "Имя должно быть от 1 до 100 символов")
        String name,

        @Size(max = 500, message = "Описание не может превышать 500 символов")
        String description,

        Long modelId   // можно обновлять модель, если нужно
) {}
