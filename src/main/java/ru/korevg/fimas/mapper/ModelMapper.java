package ru.korevg.fimas.mapper;

import org.mapstruct.*;
import ru.korevg.fimas.dto.model.ModelCreateRequest;
import ru.korevg.fimas.dto.model.ModelResponse;
import ru.korevg.fimas.dto.model.ModelUpdateRequest;
import ru.korevg.fimas.entity.Model;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ModelMapper {

    @Mapping(target = "vendor", ignore = true)
    Model toEntity(ModelCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "vendor", ignore = true)
    void updateFromRequest(ModelUpdateRequest request, @MappingTarget Model model);

    @Mapping(source = "vendor.name", target = "vendorName")
    ModelResponse toResponse(Model model);
}