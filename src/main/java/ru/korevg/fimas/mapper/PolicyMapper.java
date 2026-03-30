package ru.korevg.fimas.mapper;

import org.mapstruct.*;
import ru.korevg.fimas.dto.address.AddressShortResponse;
import ru.korevg.fimas.dto.policy.PolicyCreateRequest;
import ru.korevg.fimas.dto.policy.PolicyResponse;
import ru.korevg.fimas.dto.policy.PolicyUpdateRequest;
import ru.korevg.fimas.dto.service.ServiceShortResponse;
import ru.korevg.fimas.dto.zone.ZoneResponse;
import ru.korevg.fimas.entity.*;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PolicyMapper {

    @Mapping(target = "firewall", ignore = true)
    @Mapping(target = "srcAddresses", ignore = true)
    @Mapping(target = "dstAddresses", ignore = true)
    @Mapping(target = "services", ignore = true)
    @Mapping(target = "srcZone", ignore = true)
    @Mapping(target = "dstZone", ignore = true)
    Policy toEntity(PolicyCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "firewall", ignore = true)
    @Mapping(target = "srcAddresses", ignore = true)
    @Mapping(target = "dstAddresses", ignore = true)
    @Mapping(target = "services", ignore = true)
    @Mapping(target = "srcZone", ignore = true)
    @Mapping(target = "dstZone", ignore = true)
    void updateFromRequest(PolicyUpdateRequest request, @MappingTarget Policy policy);

    @Mapping(source = "firewall.id", target = "firewallId")
    @Mapping(source = "firewall.name", target = "firewallName")
    @Mapping(source = "srcAddresses", target = "srcAddresses")
    @Mapping(source = "dstAddresses", target = "dstAddresses")
    @Mapping(source = "services", target = "services")
    @Mapping(source = "srcZone", target = "srcZone")
    @Mapping(source = "dstZone", target = "dstZone")
    PolicyResponse toResponse(Policy policy);

    default Set<AddressShortResponse> mapAddresses(Set<Address> addresses) {
        if (addresses == null) return Set.of();
        return addresses.stream()
                .map(a -> new AddressShortResponse(a.getId(), a.getName()))
                .collect(Collectors.toSet());
    }

    default Set<ServiceShortResponse> mapServices(Set<Service> services) {
        if (services == null) return Set.of();
        return services.stream()
                .map(s -> new ServiceShortResponse(s.getId(), s.getName()))
                .collect(Collectors.toSet());
    }

    default ZoneResponse mapZone(Zone zone) {
        if (zone == null) {
            return null;                    // или можно вернуть ZoneResponse для ANY, если нужно
        }
        return new ZoneResponse(
                zone.getId(),
                zone.getName(),
                zone.getDescription(),
                zone.getPriority()
        );
    }
}
