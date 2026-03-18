package ru.korevg.fimas.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.springframework.stereotype.Component;
import ru.korevg.fimas.dto.action.ActionCreate;
import ru.korevg.fimas.dto.action.ActionResponse;
import ru.korevg.fimas.dto.action.ActionUpdate;
import ru.korevg.fimas.dto.command.CommandCreate;
import ru.korevg.fimas.dto.command.CommandResponse;
import ru.korevg.fimas.dto.command.CommandUpdate;
import ru.korevg.fimas.entity.Action;
import ru.korevg.fimas.entity.Command;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
@Component
public interface ActionCommandMapper {

    // ==================== Command ====================

    @Mapping(target = "vendorId", source = "vendor.id")
    @Mapping(target = "vendorName", source = "vendor.name")
    CommandResponse toResponse(Command command);

    Command toEntity(CommandCreate dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(CommandUpdate dto, @MappingTarget Command entity);

    List<CommandResponse> toCommandResponseList(List<Command> commands);

    // ==================== Action ====================

    @Mapping(target = "commands", source = "commands")
    ActionResponse toResponse(Action action);

    @Mapping(target = "commands", ignore = true) // команды маппим вручную
    Action toEntity(ActionCreate dto);

    @Mapping(target = "commands", ignore = true)
    Action toEntity(ActionUpdate dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "commands", ignore = true)
    void updateEntityFromDto(ActionUpdate dto, @MappingTarget Action entity);

    @IterableMapping(qualifiedByName = "toActionResponse")
    List<ActionResponse> toActionResponseList(List<Action> actions);

    @Named("toActionResponse")
    default ActionResponse mapActionToResponse(Action action) {
        return toResponse(action);
    }
}
