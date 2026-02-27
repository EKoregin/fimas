package ru.korevg.fimas.mapper;

import org.mapstruct.*;
import ru.korevg.fimas.dto.policy.PolicyCreateRequest;
import ru.korevg.fimas.dto.policy.PolicyResponse;
import ru.korevg.fimas.dto.policy.PolicyUpdateRequest;
import ru.korevg.fimas.entity.*;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PolicyMapper {

    @Mapping(target = "firewall", ignore = true)
    @Mapping(target = "srcAddresses", ignore = true)
    @Mapping(target = "dstAddresses", ignore = true)
    @Mapping(target = "services", ignore = true)
    Policy toEntity(PolicyCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "firewall", ignore = true)
    @Mapping(target = "srcAddresses", ignore = true)
    @Mapping(target = "dstAddresses", ignore = true)
    @Mapping(target = "services", ignore = true)
    void updateFromRequest(PolicyUpdateRequest request, @MappingTarget Policy policy);

    @Mapping(source = "firewall.id", target = "firewallId")
    @Mapping(source = "firewall.name", target = "firewallName")
    @Mapping(source = "srcAddresses", target = "srcAddresses")
    @Mapping(source = "dstAddresses", target = "dstAddresses")
    @Mapping(source = "services", target = "services")
    PolicyResponse toResponse(Policy policy);

    default Set<PolicyResponse.AddressShortResponse> mapAddresses(Set<Address> addresses) {
        if (addresses == null) return Set.of();
        return addresses.stream()
                .map(a -> new PolicyResponse.AddressShortResponse(a.getId(), a.getName()))
                .collect(Collectors.toSet());
    }

    default Set<PolicyResponse.ServiceShortResponse> mapServices(Set<Service> services) {
        if (services == null) return Set.of();
        return services.stream()
                .map(s -> new PolicyResponse.ServiceShortResponse(s.getId(), s.getName()))
                .collect(Collectors.toSet());
    }
}
