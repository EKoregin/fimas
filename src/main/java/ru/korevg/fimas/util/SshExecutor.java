package ru.korevg.fimas.util;

import com.jcraft.jsch.*;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

@Slf4j
@Component
public class SshExecutor {

    private Session session;

    public Session createSshSession(String host, int port, String username, String password)
            throws JSchException {
        JSch jsch = new JSch();
        session = jsch.getSession(username, host, port > 0 ? port : 22);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");   // TODO: в проде использовать known_hosts + ключ
        session.connect(15000); // 15 секунд таймаут
        return session;
    }

    public String executeSshCommand(String command) throws Exception {
        log.info("Выполняю SSH-команду: {}", command);
        ChannelExec channel = null;

        try {
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayOutputStream err = new ByteArrayOutputStream();

            channel.setOutputStream(out);
            channel.setErrStream(err);

            channel.connect(30000); // таймаут на выполнение команды

            while (!channel.isClosed()) {
                Thread.sleep(100);
            }

            String output = out.toString("UTF-8");
            String error = err.toString("UTF-8");

            return error.isEmpty() ? output : output + "\nERROR: " + error;

        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
        }
    }

    public void disconnect() {
        if (session != null && session.isConnected()) {
            try {
                session.disconnect();
                log.debug("SSH-сессия успешно закрыта");
            } catch (Exception ex) {
                log.warn("Ошибка при закрытии SSH-сессии: {}", ex.getMessage());
            }
        }
    }
}
