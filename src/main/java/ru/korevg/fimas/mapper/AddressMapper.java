package ru.korevg.fimas.mapper;

import org.mapstruct.*;
import ru.korevg.fimas.dto.address.*;
import ru.korevg.fimas.entity.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AddressMapper {

    // Common
    CommonAddress toCommonEntity(AddressCommonCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateCommonFromRequest(AddressCommonCreateRequest request, @MappingTarget CommonAddress address);

    // Dynamic
    @Mapping(target = "firewall", ignore = true)  // будем устанавливать вручную в сервисе
    DynamicAddress toDynamicEntity(AddressDynamicCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateDynamicFromRequest(AddressDynamicCreateRequest request, @MappingTarget DynamicAddress address);

    // ────────────────────────────────────────────────
    // Общий метод ответа — используем @AfterMapping
    // ────────────────────────────────────────────────
    default AddressResponse toResponse(Address address) {
        if (address instanceof CommonAddress common) {
            return toCommonResponse(common);
        } else if (address instanceof DynamicAddress dynamic) {
            return toDynamicResponse(dynamic);
        }
        throw new IllegalArgumentException("Неизвестный тип адреса: " + address.getClass().getName());
    }

    // Ответ для CommonAddress
    @Mapping(target = "addressType", expression = "java(\"COMMON\")")
    @Mapping(target = "firewallId", ignore = true)
    @Mapping(target = "firewallName", ignore = true)
    AddressResponse toCommonResponse(CommonAddress address);

    // Ответ для DynamicAddress
    @Mapping(target = "addressType", expression = "java(\"DYNAMIC\")")
    @Mapping(source = "firewall.id", target = "firewallId")
    @Mapping(source = "firewall.name", target = "firewallName")
    AddressResponse toDynamicResponse(DynamicAddress address);
}
