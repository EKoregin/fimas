package ru.korevg.fimas.mapper;

import org.mapstruct.*;
import ru.korevg.fimas.dto.port.PortCreateRequest;
import ru.korevg.fimas.dto.port.PortResponse;
import ru.korevg.fimas.dto.port.PortShortResponse;
import ru.korevg.fimas.dto.port.PortUpdateRequest;
import ru.korevg.fimas.entity.Port;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PortMapper {

    Port toEntity(PortCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromRequest(PortUpdateRequest request, @MappingTarget Port port);

    PortResponse toResponse(Port port);

    PortShortResponse toShortResponse(Port port);
}
