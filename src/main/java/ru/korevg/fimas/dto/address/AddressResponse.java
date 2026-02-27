package ru.korevg.fimas.dto.address;

import java.util.Set;

public record AddressResponse(
        Long id,
        String addressType,      // "COMMON" или "DYNAMIC"
        String name,
        String description,
        Set<String> addresses,
        Long firewallId,         // null для Common
        String firewallName      // null для Common
) {}
