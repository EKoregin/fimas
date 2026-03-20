package ru.korevg.fimas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.korevg.fimas.dto.action.ActionCreate;
import ru.korevg.fimas.dto.action.ActionResponse;
import ru.korevg.fimas.dto.action.ActionUpdate;
import ru.korevg.fimas.dto.command.CommandCreate;
import ru.korevg.fimas.dto.command.CommandResponse;
import ru.korevg.fimas.dto.command.CommandUpdate;
import ru.korevg.fimas.entity.Action;
import ru.korevg.fimas.entity.Command;
import ru.korevg.fimas.mapper.ActionCommandMapper;
import ru.korevg.fimas.repository.ActionRepository;
import ru.korevg.fimas.repository.CommandRepository;
import ru.korevg.fimas.repository.ModelRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActionCommandService {

    private final ActionRepository actionRepository;
    private final CommandRepository commandRepository;
    private final ModelRepository modelRepository;

    @Qualifier("actionCommandMapper")
    private final ActionCommandMapper mapper;

    // ==================== Action ====================

    public List<ActionResponse> getAllActions() {
        return mapper.toActionResponseList(actionRepository.findAll());
    }

    public Optional<ActionResponse> getActionById(Long id) {
        return actionRepository.findByIdWithCommands(id)
                .map(mapper::toResponse);
    }

    @Transactional
    public ActionResponse createAction(ActionCreate createDto) {
        Action action = mapper.toEntity(createDto);

        // Загружаем команды по ID
        List<Command> commands = commandRepository.findAllById(createDto.getCommandIds());
        action.getCommands().clear();
        action.getCommands().addAll(commands);

        Action savedAction = actionRepository.save(action);
        return mapper.toResponse(savedAction);
    }

    @Transactional
    public ActionResponse updateAction(Long id, ActionUpdate updateDto) {
        Action action = actionRepository.findByIdWithCommands(id)
                .orElseThrow(() -> new RuntimeException("Action not found with id: " + id));

        mapper.updateEntityFromDto(updateDto, action);

        if (updateDto.getCommandIds() != null) {
            List<Command> commands = commandRepository.findAllById(updateDto.getCommandIds());
            action.getCommands().clear();
            action.getCommands().addAll(commands);
        }

        Action saved = actionRepository.save(action);
        return mapper.toResponse(saved);
    }

    @Transactional
    public void deleteAction(Long id) {
        actionRepository.deleteById(id);
    }

    public List<ActionResponse> getActionsByModel(Long modelId) {
        return modelRepository.findByIdWithActions(modelId)
                .map(model -> mapper.toActionResponseList(new ArrayList<>(model.getActions())))
                .orElse(List.of());
    }


    // ==================== Command ====================

    public List<CommandResponse> getAllCommands() {
        return mapper.toCommandResponseList(commandRepository.findAll());
    }

    public Optional<CommandResponse> getCommandById(Long id) {
        return commandRepository.findById(id).map(mapper::toResponse);
    }

    public List<CommandResponse> getCommandsByVendor(Long vendorId) {
        return mapper.toCommandResponseList(commandRepository.findByVendorId(vendorId));
    }

    @Transactional
    public CommandResponse createCommand(CommandCreate createDto) {
        Command command = mapper.toEntity(createDto);
        Command saved = commandRepository.save(command);
        return mapper.toResponse(saved);
    }

    @Transactional
    public CommandResponse updateCommand(Long id, CommandUpdate updateDto) {
        Command command = commandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Command not found with id: " + id));

        mapper.updateEntityFromDto(updateDto, command);
        Command saved = commandRepository.save(command);
        return mapper.toResponse(saved);
    }

    @Transactional
    public void deleteCommand(Long id) {
        commandRepository.deleteById(id);
    }
}
