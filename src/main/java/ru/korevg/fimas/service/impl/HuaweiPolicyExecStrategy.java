package ru.korevg.fimas.service.impl;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.korevg.fimas.entity.Action;
import ru.korevg.fimas.entity.Command;
import ru.korevg.fimas.entity.CommandType;
import ru.korevg.fimas.service.PolicyExecStrategy;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class HuaweiPolicyExecStrategy implements PolicyExecStrategy {

    @Override
    public List<String> execute(Action action, String host, int port, String username, String password) throws Exception {
        List<String> results = new ArrayList<>();

        Session session = null;
        ChannelShell channel = null;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(username, host, port > 0 ? port : 22);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no"); // В продакшене → known_hosts + public key
            session.connect(30_000);

            channel = (ChannelShell) session.openChannel("shell");
            channel.connect(10_000);

            // Потоки для ввода/вывода
            OutputStream output = channel.getOutputStream();
            InputStream input = channel.getInputStream();

            // Ждём начального промпта и входим в систему (если нужно)
            readUntil(input, ">");  // или "#" если уже привилегированный режим

            // Переходим в system-view (аналог configure terminal)
            sendCommand(output, "system-view\n");
            readUntil(input, "]");  // ждём [Huawei]

            log.info("Execute action: {}", action.getName());
            for (Command cmd : action.getCommands()) {
                log.info("Executing command: {}", cmd.getCommand());
                String result;
                if (cmd.getCommandType() == CommandType.SSH) {
                    result = executeHuaweiCliCommand(output, input, cmd.getCommand());
                } else if (cmd.getCommandType() == CommandType.HTTPS) {
                    result = executeHttpsHuaweiApi(host, cmd.getCommand(), username, password);
                } else {
                    result = "Неизвестный тип команды: " + cmd.getCommandType();
                }

                results.add(String.format(
                        "=== %s (%s) ===\n%s\n",
                        cmd.getName(), cmd.getCommandType(), result.trim()
                ));
            }

        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }

        return results;
    }

    @Override
    public String getSupportedKey() {
        return "huawei";  // или "usg", "vrp", в зависимости от вашей логики выбора стратегии
    }

    /**
     * Выполнение одной команды в Huawei CLI через shell-канал
     * (более надёжно, чем ChannelExec для интерактивного CLI)
     */
    private String executeHuaweiCliCommand(OutputStream output, InputStream input, String command) throws Exception {
        log.info("Выполняю команду на Huawei: {}", command);

        // Отправляем команду + enter
        sendCommand(output, command + "\n");

        // Читаем вывод до следующего промпта
        String outputText = readUntil(input, "]");  // предполагаем, что промпт заканчивается на ]

        // Убираем эхо команды и сам промпт из результата
        int commandEndIndex = outputText.indexOf(command) + command.length();
        if (commandEndIndex < outputText.length()) {
            outputText = outputText.substring(commandEndIndex).trim();
        }

        // Убираем последний промпт
        int lastPrompt = outputText.lastIndexOf("]");
        if (lastPrompt > 0) {
            outputText = outputText.substring(0, lastPrompt).trim();
        }

        return outputText.isEmpty() ? "<команда выполнена без вывода>" : outputText;
    }

    private void sendCommand(OutputStream output, String command) throws IOException {
        output.write(command.getBytes());
        output.flush();
    }

    private String readUntil(InputStream input, String endMarker) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] tmp = new byte[1024];
        long timeout = System.currentTimeMillis() + 30_000; // 30 секунд таймаут

        while (System.currentTimeMillis() < timeout) {
            while (input.available() > 0) {
                int len = input.read(tmp);
                if (len < 0) break;
                buffer.write(tmp, 0, len);
            }

            String current = buffer.toString("UTF-8");
            if (current.contains(endMarker)) {
                return current;
            }

            Thread.sleep(200);
        }

        throw new TimeoutException("Таймаут ожидания промпта: " + endMarker);
    }

    /** Placeholder для Huawei Northbound REST API (HTTPS) */
    private String executeHttpsHuaweiApi(String host, String apiPath, String username, String password) {
        // Реальная реализация сложнее:
        // 1. POST /rest/login для получения X-Auth-Token (или session token)
        // 2. Использовать токен в заголовке X-Auth-Token для последующих запросов
        // 3. API часто возвращает JSON с кодом ошибки

        try {
            // Пример URL (реальный путь зависит от версии USG)
            String url = "https://" + host + ":443/rest/" + apiPath;  // часто /rest/...
            // Нужно добавить авторизацию, обработку HTTPS с самоподписанным сертификатом и т.д.

            return "Huawei REST API вызван: " + url +
                    "\n(реальная авторизация: POST /rest/login → token → запросы с X-Auth-Token)" +
                    "\nПолная реализация требует HttpClient/RestTemplate + токен-менеджмент";
        } catch (Exception e) {
            return "Ошибка HTTPS Huawei API: " + e.getMessage();
        }
    }
}