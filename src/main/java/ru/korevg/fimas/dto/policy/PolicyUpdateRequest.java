package ru.korevg.fimas.dto.policy;

import jakarta.validation.constraints.Size;
import ru.korevg.fimas.entity.PolicyAction;
import ru.korevg.fimas.entity.PolicyStatus;

import java.util.Set;

public record PolicyUpdateRequest(

        @Size(min = 1, max = 100, message = "Имя от 1 до 100 символов")
        String name,

        @Size(max = 500, message = "Описание не более 500 символов")
        String description,

        PolicyAction action,
        PolicyStatus status,

        Long firewallId,

        Set<Long> srcAddressIds,
        Set<Long> dstAddressIds,
        Set<Long> serviceIds
) {}