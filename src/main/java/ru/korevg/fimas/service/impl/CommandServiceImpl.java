package ru.korevg.fimas.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.korevg.fimas.dto.command.CommandCreate;
import ru.korevg.fimas.dto.command.CommandResponse;
import ru.korevg.fimas.dto.command.CommandUpdate;
import ru.korevg.fimas.entity.Command;
import ru.korevg.fimas.exception.EntityNotFoundException;
import ru.korevg.fimas.mapper.ActionCommandMapper;
import ru.korevg.fimas.repository.ActionRepository;
import ru.korevg.fimas.repository.CommandRepository;
import ru.korevg.fimas.service.CommandService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommandServiceImpl implements CommandService {

    private final CommandRepository commandRepository;
    private final ActionCommandMapper actionCommandMapper;
    private final ActionRepository actionRepository;

    @Override
    public List<CommandResponse> getCommandsByVendor(Long vendorId) {
        return actionCommandMapper.toCommandResponseList(commandRepository.findByVendorId(vendorId));
    }


    @Override
    public List<CommandResponse> findAll() {
        log.info("Поиск всех команд для определенного вендора");
        return commandRepository.findAll()
                .stream()
                .map(actionCommandMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<CommandResponse> findById(Long aLong) {
        return commandRepository.findById(aLong).map(actionCommandMapper::toResponse);
    }

    @Override
    @Transactional
    public CommandResponse create(CommandCreate request) {
        log.info("Создание команды {}", request.toString());
        var newCommand = commandRepository.save(actionCommandMapper.toEntity(request));
        return actionCommandMapper.toResponse(newCommand);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCommandUsedInAnyAction(Long commandId) {
        return commandId != null && actionRepository.existsByCommandsId(commandId);
    }

    @Override
    @Transactional
    public void deleteCommand(Long commandId) {
        if (isCommandUsedInAnyAction(commandId)) {
            throw new IllegalStateException("Нельзя удалить команду — она используется в действиях");
        }
        commandRepository.deleteById(commandId);
    }

    @Override
    @Transactional
    public CommandResponse updateCommand(Long commandId, CommandUpdate updateDto) {
        Command command = commandRepository.findById(commandId)
                .orElseThrow(() -> new EntityNotFoundException("Команда не найдена"));

        if (isCommandUsedInAnyAction(commandId)) {
            throw new IllegalStateException("Нельзя редактировать команду — она используется в действиях");
        }

        // Частичное обновление
        if (updateDto.getName() != null) {
            command.setName(updateDto.getName());
        }
        if (updateDto.getCommand() != null) {
            command.setCommand(updateDto.getCommand());
        }
        if (updateDto.getCommandType() != null) {
            command.setCommandType(updateDto.getCommandType());
        }

        Command saved = commandRepository.save(command);
        return actionCommandMapper.toResponse(saved);
    }
}
