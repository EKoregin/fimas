package ru.korevg.fimas.dto.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import ru.korevg.fimas.validation.ValidInet;

import java.util.Set;

@Builder
@Getter
@Setter
public class AddressCommonCreateRequest {

        @NotBlank
        @Size(max = 100)
        String name;

        @Size(max = 500)
        String description;

        @Size(max = 10)
        String subType;

        Set<@ValidInet(message = "Некорректный IP-адрес или подсеть") String> addresses;
}