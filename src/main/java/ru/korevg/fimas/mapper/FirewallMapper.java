package ru.korevg.fimas.mapper;

import org.mapstruct.*;
import ru.korevg.fimas.dto.firewall.FirewallCreateRequest;
import ru.korevg.fimas.dto.firewall.FirewallResponse;
import ru.korevg.fimas.dto.firewall.FirewallUpdateRequest;
import ru.korevg.fimas.entity.Firewall;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FirewallMapper {

    @Mapping(target = "model", ignore = true)  // модель будем устанавливать вручную
    Firewall toEntity(FirewallCreateRequest request);

    @Mapping(target = "model", ignore = true)
    @Mapping(target = "policies", ignore = true)
    @Mapping(target = "dynamicAddresses", ignore = true)
    void updateFromRequest(FirewallUpdateRequest request, @MappingTarget Firewall firewall);

    @Mapping(source = "model.name", target = "modelName")
    @Mapping(source = "model.vendor.name", target = "vendorName")
    FirewallResponse toResponse(Firewall firewall);
}
