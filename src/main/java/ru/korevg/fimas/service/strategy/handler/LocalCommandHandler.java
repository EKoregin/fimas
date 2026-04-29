package ru.korevg.fimas.service.strategy.handler;

import ru.korevg.fimas.entity.Command;

public interface LocalCommandHandler {

    String handle(Command command, Long firewallId, String username, String password);
    String getCommandKey();
    String getVendorKey();


}