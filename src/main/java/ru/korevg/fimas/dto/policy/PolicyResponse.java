package ru.korevg.fimas.dto.policy;

import ru.korevg.fimas.entity.PolicyAction;
import ru.korevg.fimas.entity.PolicyStatus;

import java.util.Set;

public record PolicyResponse(
        Long id,
        String name,
        String description,
        PolicyAction action,
        PolicyStatus status,
        Long firewallId,
        String firewallName,
        Set<AddressShortResponse> srcAddresses,
        Set<AddressShortResponse> dstAddresses,
        Set<ServiceShortResponse> services
) {
    public record AddressShortResponse(Long id, String name) {}
    public record ServiceShortResponse(Long id, String name) {}
}