package ru.korevg.fimas.mapper;

import org.mapstruct.*;
import ru.korevg.fimas.dto.service.ServiceCreateRequest;
import ru.korevg.fimas.dto.service.ServiceResponse;
import ru.korevg.fimas.dto.service.ServiceUpdateRequest;
import ru.korevg.fimas.entity.Port;
import ru.korevg.fimas.entity.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ServiceMapper {

    @Mapping(target = "ports", ignore = true)
    Service toEntity(ServiceCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "ports", ignore = true)
    void updateFromRequest(ServiceUpdateRequest request, @MappingTarget Service service);

    @Mapping(source = "ports", target = "ports")
    ServiceResponse toResponse(Service service);

    default Set<ServiceResponse.PortShortResponse> mapPorts(Set<Port> ports) {
        if (ports == null) return Set.of();
        return ports.stream()
                .map(p -> new ServiceResponse.PortShortResponse(
                        p.getId(),
                        p.getProtocol().name(),
                        p.getSrcPort(),
                        p.getDstPort()))
                .collect(Collectors.toSet());
    }
}