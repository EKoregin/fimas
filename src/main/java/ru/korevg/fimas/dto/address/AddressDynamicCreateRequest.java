package ru.korevg.fimas.dto.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record AddressDynamicCreateRequest(

        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        Set<String> addresses,

        @NotNull Long firewallId
) {}