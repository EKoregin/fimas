package ru.korevg.fimas.dto.firewall;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FirewallCreateRequest(

        @NotBlank(message = "Имя firewall обязательно")
        @Size(min = 1, max = 100, message = "Имя должно быть от 1 до 100 символов")
        String name,

        @Size(max = 500, message = "Описание не может превышать 500 символов")
        String description,

        @NotNull(message = "ID модели обязателен")
        Long modelId,

        @NotNull(message = "IP управления обязателен")
        String mgmtIpAddress
) {}
