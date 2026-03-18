package ru.korevg.fimas.service;

import ru.korevg.fimas.entity.Action;

import java.util.List;

public interface PolicyExecStrategy {

    /**
     * Выполняет все команды Action по порядку.
     * @return список результатов (output каждой команды)
     */
    List<String> execute(Action action, String host, int port, String username, String password) throws Exception;
}
