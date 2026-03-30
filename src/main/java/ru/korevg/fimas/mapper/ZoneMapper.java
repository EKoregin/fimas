package ru.korevg.fimas.mapper;

import org.mapstruct.Mapper;
import ru.korevg.fimas.dto.zone.ZoneResponse;
import ru.korevg.fimas.entity.Zone;

@Mapper(componentModel = "spring")
public interface ZoneMapper {
    ZoneResponse toResponse(Zone zone);
}
