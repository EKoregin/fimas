package ru.korevg.fimas.service;

import ru.korevg.fimas.dto.command.CommandCreate;
import ru.korevg.fimas.dto.command.CommandResponse;
import ru.korevg.fimas.dto.command.CommandUpdate;

import java.util.List;
import java.util.Optional;

public interface CommandService {
    List<CommandResponse> getCommandsByVendor(Long vendorId);

    List<CommandResponse> findAll();

    Optional<CommandResponse> findById(Long aLong);

    CommandResponse create(CommandCreate request);


    boolean isCommandUsedInAnyAction(Long commandId);

    void deleteCommand(Long commandId);

    CommandResponse updateCommand(Long commandId, CommandUpdate updateDto);
}
