package ru.korevg.fimas.service.strategy.handler;

import ru.korevg.fimas.entity.Command;

public interface LocalCommandHandler {

    String handle(Command command);
    String getCommandKey();
    String getVendorKey();
}