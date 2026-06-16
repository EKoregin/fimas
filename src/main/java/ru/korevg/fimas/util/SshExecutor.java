package ru.korevg.fimas.util;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class SshExecutor {

    private static final int DEFAULT_CONNECT_TIMEOUT = 15_000;
    private static final int DEFAULT_COMMAND_TIMEOUT = 30_000;
    private static final String STRICT_HOST_KEY_CHECKING = "StrictHostKeyChecking";

    private Session session;

    public Session createSshSession(String host, int port, String username, String password)
            throws JSchException {
        JSch jsch = new JSch();
        int actualPort = port > 0 ? port : 22;

        session = jsch.getSession(username, host, actualPort);
        session.setPassword(password);
        session.setConfig(STRICT_HOST_KEY_CHECKING, "no");   // TODO: в проде использовать known_hosts + ключ

        log.info("Подключение к {}@{}:{}", username, host, actualPort);
        session.connect(DEFAULT_CONNECT_TIMEOUT);

        return session;
    }

    /**
     * Выполнение одной команды
     */
    public String executeSshCommand(String command) throws JSchException {
        return executeCommand(command, DEFAULT_COMMAND_TIMEOUT);
    }

    public String executeCommand(String command, int timeoutMillis) throws JSchException {
        Objects.requireNonNull(command, "command cannot be null");
        checkSession();

        log.info("Выполняю SSH-команду: {}", command);
        return executeOnChannel(command, timeoutMillis);
    }

    /**
     * Выполнение списка команд. По очереди
     */
    public String executeSshCommands(List<String> commands) throws JSchException {
        return executeCommands(commands, DEFAULT_COMMAND_TIMEOUT);
    }

    public String executeCommands(List<String> commands, int timeoutMillisPerCommand) throws JSchException {
        Objects.requireNonNull(commands, "commands cannot be null");
        checkSession();

        if (commands.isEmpty()) {
            return "";
        }

        log.info("Выполняю {} команд через ChannelShell на {}", commands.size(), session.getHost());

        ChannelShell channel = null;
        try {
            channel = openShellChannel();
            OutputStream output = channel.getOutputStream();
            InputStream input = channel.getInputStream();

            channel.connect(DEFAULT_CONNECT_TIMEOUT);

            StringBuilder fullResult = new StringBuilder();
            long globalDeadline = System.currentTimeMillis() + (timeoutMillisPerCommand * commands.size() * 2L); // страховка

            for (String rawCmd : commands) {
                String cmd = rawCmd.trim();
                if (cmd.isEmpty()) continue;

                log.info("Отправка: {}", cmd);

                // Уникальный маркер — ключ к скорости!
                String marker = "===CMD_DONE_" + System.currentTimeMillis() + "===";
                String commandToSend = cmd + "\necho \"" + marker + "\"\n";

                output.write(commandToSend.getBytes(StandardCharsets.UTF_8));
                output.flush();

                // Читаем до маркера или таймаута
                String outputPart = readUntilMarker(input, marker, timeoutMillisPerCommand);
                fullResult.append(outputPart);
            }

            output.write("exit\n".getBytes(StandardCharsets.UTF_8));
            output.flush();

            return fullResult.toString();

        } catch (Exception e) {
            log.error("Ошибка выполнения через ChannelShell", e);
            throw new JSchException("ChannelShell failed: " + e.getMessage(), e);
        } finally {
            closeChannelQuietly(channel);
        }
    }

    private String readUntilMarker(InputStream in, String marker, int timeoutMillis) throws IOException, InterruptedException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
        byte[] buffer = new byte[8192];
        long deadline = System.currentTimeMillis() + timeoutMillis;
        StringBuilder bufferForSearch = new StringBuilder();

        while (System.currentTimeMillis() < deadline) {
            if (in.available() > 0) {
                int bytesRead = in.read(buffer);
                if (bytesRead > 0) {
                    String chunk = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                    baos.write(buffer, 0, bytesRead);
                    bufferForSearch.append(chunk);

                    // Проверяем маркер
                    if (bufferForSearch.indexOf(marker) != -1) {
                        log.debug("Маркер найден — команда завершена");
                        return baos.toString(StandardCharsets.UTF_8);
                    }
                }
            } else {
                Thread.sleep(50); // маленькая пауза, когда данных нет
            }
        }

        log.warn("Таймаут ожидания маркера");
        return baos.toString(StandardCharsets.UTF_8);
    }

    private ChannelShell openShellChannel() throws JSchException {
        log.info("Open new Shell channel in session: {}", session.getHost());
        ChannelShell channel = (ChannelShell) session.openChannel("shell");
        channel.setPty(true);                    // Очень важно для многих устройств!
        channel.setPtySize(120, 500, 0, 0);
        return channel;
    }

    private ChannelExec openExecChannel() throws JSchException {
        log.info("Open new channel in session: {}", session.getHost());
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setPty(false); // важно для exec-канала
        return channel;
    }

    private String executeOnChannel(String command, int timeoutMillis) throws JSchException {
        ChannelExec channel = null;
        try {
            channel = openExecChannel();
            log.info("Exec command: {}  in channel: {}", command, channel.getId());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayOutputStream err = new ByteArrayOutputStream();

            channel.setCommand(command);
            channel.setOutputStream(out);
            channel.setErrStream(err);
            channel.connect(timeoutMillis);

            waitForChannelClose(channel, timeoutMillis);

            String output = out.toString(StandardCharsets.UTF_8);
            String error = err.toString(StandardCharsets.UTF_8);

            if (!error.isBlank()) {
                return output + "\nERROR: " + error;
            }
            return output;

        } finally {
            if (channel != null) {
                log.info("Try to close channel: {}", channel.getId());
            }
            closeChannelQuietly(channel);
        }
    }

    private void waitForChannelClose(ChannelExec channel, int timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;

        try {
            while (!channel.isClosed()) {
                if (System.currentTimeMillis() > deadline) {
                    log.warn("Команда не завершилась в отведённый таймаут");
                    break;
                }
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Ожидание завершения команды было прервано");
        }
    }

    private void closeChannelQuietly(ChannelShell channel) {
        if (channel != null && channel.isConnected()) {
            try {
                channel.disconnect();
                log.info("Shell channel closed");
            } catch (Exception e) {
                log.debug("Ошибка закрытия ChannelShell", e);
            }
        } else {
            log.info("Channel {} already closed", channel.getId());
        }
    }

    private void closeChannelQuietly(ChannelExec channel) {
        if (channel != null && channel.isConnected()) {
            try {
                channel.disconnect();
                log.info("Channel {} closed", channel.getId());
            } catch (Exception e) {
                log.debug("Ошибка при закрытии ChannelExec", e);
            }
        } else {
            log.info("Channel {} already closed", channel.getId());
        }
    }

    private void checkSession() {
        if (session == null || !session.isConnected()) {
            throw new IllegalStateException("SSH сессия не создана или не подключена. " +
                    "Сначала вызовите createSshSession()");
        }
    }

    public void disconnect() {
        if (session != null && session.isConnected()) {
            try {
                session.disconnect();
                log.debug("SSH-сессия успешно закрыта");
            } catch (Exception e) {
                log.warn("Ошибка при закрытии SSH-сессии", e);
            } finally {
                session = null;
            }
        }
    }
}
