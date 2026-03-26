package ru.korevg.fimas.service.strategy.handler;

import ru.korevg.fimas.entity.Command;

public interface LocalCommandHandler {

    /**
     * Выполняет локальную логику для конкретной команды
     */
    String handle(Command command);

    /**
     * По какому ключу регистрируется обработчик.
     * Можно возвращать command.getCommand() из сущности, либо фиксированную строку.
     */
    String getCommandKey();
}