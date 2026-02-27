package ru.korevg.fimas.mapper;

import org.mapstruct.*;
import ru.korevg.fimas.dto.vendor.VendorCreateRequest;
import ru.korevg.fimas.dto.vendor.VendorResponse;
import ru.korevg.fimas.dto.vendor.VendorUpdateRequest;
import ru.korevg.fimas.entity.Vendor;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VendorMapper {

    Vendor toEntity(VendorCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromRequest(VendorUpdateRequest request, @MappingTarget Vendor vendor);

    VendorResponse toResponse(Vendor vendor);
}