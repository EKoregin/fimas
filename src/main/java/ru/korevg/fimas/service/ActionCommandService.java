package ru.korevg.fimas.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import ru.korevg.fimas.entity.Model;
import ru.korevg.fimas.exception.EntityNotFoundException;
import ru.korevg.fimas.mapper.ActionCommandMapper;
import ru.korevg.fimas.repository.ActionRepository;
import ru.korevg.fimas.repository.CommandRepository;
import ru.korevg.fimas.repository.ModelRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
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
        log.info("Создание Action ={}, кол-во команд={}",
                createDto.toString(),
                createDto.getCommandIds() != null ? createDto.getCommandIds().size() : "не изменено");
        Model model = modelRepository.findById(createDto.getModelId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Модель с ID " + createDto.getModelId() + " не найдена"
                ));

        Action action = mapper.toEntity(createDto);

        List<Command> commands = commandRepository.findAllById(createDto.getCommandIds());
        if (commands.size() != createDto.getCommandIds().size()) {
            throw new IllegalArgumentException("Некоторые ID команд не существуют");
        }

        action.getCommands().addAll(commands);
        model.getActions().add(action);

        Action savedAction = actionRepository.save(action);
        return mapper.toResponse(savedAction);
    }

    @Transactional
    public ActionResponse updateAction(Long id, ActionUpdate updateDto) {
        log.info("Обновление Action, id={}, Action={}, кол-во команд={}",
                id, updateDto.toString(),
                updateDto.getCommandIds() != null ? updateDto.getCommandIds().size() : "не изменено");
        Action action = actionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Action с ID " + id + " не найден"));

        // Обновление имени (если передано)
        if (updateDto.getName() != null && !updateDto.getName().isBlank()) {
            action.setName(updateDto.getName().trim());
        }

        // Обновление команд (если передан список)
        if (updateDto.getCommandIds() != null) {
            if (updateDto.getCommandIds().isEmpty()) {
                action.getCommands().clear(); // разрешить очистить список
            } else {
                List<Command> newCommands = commandRepository.findAllById(updateDto.getCommandIds());

                if (newCommands.size() != updateDto.getCommandIds().size()) {
                    throw new IllegalArgumentException("Один или несколько ID команд не существуют");
                }

                action.getCommands().clear();
                action.getCommands().addAll(newCommands);
            }
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
//
//    public List<CommandResponse> getAllCommands() {
//        return mapper.toCommandResponseList(commandRepository.findAll());
//    }
//
//    public Optional<CommandResponse> getCommandById(Long id) {
//        return commandRepository.findById(id).map(mapper::toResponse);
//    }
//
//    public List<CommandResponse> getCommandsByVendor(Long vendorId) {
//        return mapper.toCommandResponseList(commandRepository.findByVendorId(vendorId));
//    }
//
//    @Transactional
//    public CommandResponse createCommand(CommandCreate createDto) {
//        Command command = mapper.toEntity(createDto);
//        Command saved = commandRepository.save(command);
//        return mapper.toResponse(saved);
//    }
//
//    @Transactional
//    public CommandResponse updateCommand(Long id, CommandUpdate updateDto) {
//        Command command = commandRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Command not found with id: " + id));
//
//        mapper.updateEntityFromDto(updateDto, command);
//        Command saved = commandRepository.save(command);
//        return mapper.toResponse(saved);
//    }
//
//    @Transactional
//    public void deleteCommand(Long id) {
//        commandRepository.deleteById(id);
//    }
}
