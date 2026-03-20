package ru.korevg.fimas.service.impl;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.korevg.fimas.entity.Action;
import ru.korevg.fimas.entity.Command;
import ru.korevg.fimas.entity.CommandType;
import ru.korevg.fimas.service.PolicyExecStrategy;

import java.io.*;
import java.util.*;

@Slf4j
@Component
public class FortigatePolicyExecStrategy implements PolicyExecStrategy {

    @Override
    public List<String> execute(Action action, String host, int port, String username, String password) throws Exception {
        List<String> results = new ArrayList<>();

        Session session = null;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(username, host, port > 0 ? port : 22);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no"); // в проде используйте known_hosts + ключ
            session.connect(30_000);

            for (Command cmd : action.getCommands()) {   // порядок гарантирован @OrderColumn
                String result;
                if (cmd.getCommandType() == CommandType.SSH) {
                    result = executeSshCommand(session, cmd.getCommand());
                } else if (cmd.getCommandType() == CommandType.HTTPS) {
                    result = executeHttpsFortigateApi(host, cmd.getCommand(), username, password);
                } else {
                    result = "Неизвестный тип команды: " + cmd.getCommandType();
                }

                results.add(String.format(
                        "=== %s (%s) ===\n%s\n",
                        cmd.getName(), cmd.getCommandType(), result.trim()
                ));
            }
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
        return results;
    }

    @Override
    public String getSupportedKey() {
        return "fortigate";
    }

    /** Выполнение одной SSH-команды (Fortigate CLI) */
    private String executeSshCommand(Session session, String command) throws Exception {
        log.info("Выполняю команду: {} на {}", command, session.getHost());
        ChannelExec channel = null;
        try {
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayOutputStream err = new ByteArrayOutputStream();
            channel.setOutputStream(out);
            channel.setErrStream(err);

            channel.connect();

            // ждём завершения
            while (!channel.isClosed()) {
                Thread.sleep(100);
            }

            String output = out.toString("UTF-8");
            String error = err.toString("UTF-8");

            return error.isEmpty() ? output : output + "\nERROR: " + error;
        } finally {
            if (channel != null) channel.disconnect();
        }
    }

    /** Placeholder для Fortigate REST API (HTTPS) */
    private String executeHttpsFortigateApi(String host, String apiPath, String username, String password) {
        // В реальном проекте:
        // 1. Сначала POST /api/v2/auth/login → получаем X-Auth-Token (или используйте API-ключ)
        // 2. Затем GET/POST по apiPath
        // Здесь простой пример с RestTemplate:

        try {
            RestTemplate rest = new RestTemplate();
            // Добавьте заголовки и токен в реальной реализации
            String url = "https://" + host + apiPath;
            // String result = rest.getForObject(url, String.class); // + headers
            return "HTTPS Fortigate API вызван: " + url + "\n(полная реализация с токеном — в следующем шаге)";
        } catch (Exception e) {
            return "HTTPS ошибка: " + e.getMessage();
        }
    }
}