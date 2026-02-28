package ru.korevg.fimas.dto.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ru.korevg.fimas.validation.ValidInet;

import java.util.Set;

public record AddressCommonCreateRequest(

        @NotBlank @Size(max = 100)
        String name,

        @Size(max = 500)
        String description,

        Set<@ValidInet(message = "Некорректный IP-адрес или подсеть") String> addresses
) {}