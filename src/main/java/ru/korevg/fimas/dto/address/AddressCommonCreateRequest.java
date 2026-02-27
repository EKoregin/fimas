package ru.korevg.fimas.dto.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record AddressCommonCreateRequest(

        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        Set<String> addresses
) {}