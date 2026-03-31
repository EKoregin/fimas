package ru.korevg.fimas.dto.policy;

import ru.korevg.fimas.dto.address.AddressShortResponse;
import ru.korevg.fimas.dto.service.ServiceShortResponse;
import ru.korevg.fimas.dto.zone.ZoneResponse;
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
        Set<ServiceShortResponse> services,
        Integer policyOrder,
        ZoneResponse srcZone,
        ZoneResponse dstZone,
        Boolean isLogging,
        Boolean isNat
) {}