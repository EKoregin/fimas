package ru.korevg.fimas.service.strategy;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.korevg.fimas.config.AppConstants;
import ru.korevg.fimas.config.LocalCommandHandlerRegistry;
import ru.korevg.fimas.entity.Action;
import ru.korevg.fimas.entity.Command;
import ru.korevg.fimas.entity.CommandType;
import ru.korevg.fimas.entity.Firewall;
import ru.korevg.fimas.repository.FirewallRepository;
import ru.korevg.fimas.service.strategy.handler.LocalCommandHandler;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class FortigatePolicyExecStrategy implements PolicyExecStrategy {

    private final RestTemplate restTemplate = new RestTemplate(); // можно заинжектить, если нужно
    private final LocalCommandHandlerRegistry localHandlerRegistry;

    public FortigatePolicyExecStrategy(LocalCommandHandlerRegistry localHandlerRegistry) {
        this.localHandlerRegistry = localHandlerRegistry;
    }

    @Override
    public List<String> execute(Long firewallId, Action action, String vendorKey, String host, int port, String username, String password) throws Exception {
        List<String> results = new ArrayList<>();

        // Определяем, нужен ли SSH-сессия вообще
        boolean needsSsh = action.getCommands().stream()
                .anyMatch(cmd -> cmd.getCommandType() == CommandType.SSH);

        Session session = null;

        try {
            // Создаём SSH-сессию ТОЛЬКО если она действительно нужна
            if (needsSsh) {
                log.info("Выполнение действия '{}' на хосте {}:{}", action.getName(), host, port);
                session = createSshSession(host, port, username, password);
                log.debug("SSH-сессия успешно установлена с {}", host);
            }

            for (Command cmd : action.getCommands()) {
                String result;

                switch (cmd.getCommandType()) {
                    case SSH:
                        if (session == null) {
                            throw new IllegalStateException("SSH сессия не была создана, хотя есть SSH-команда");
                        }
                        result = executeSshCommand(session, cmd.getCommand());
                        break;

                    case HTTPS:
                        result = executeHttpsFortigateApi(host, cmd.getCommand(), username, password);
                        break;

                    case LOCAL:
                        result = executeLocalHandler(cmd, vendorKey, firewallId);
                        break;

                    default:
                        result = "Неизвестный тип команды: " + cmd.getCommandType();
                        log.warn("Неизвестный тип команды: {}", cmd.getCommandType());
                }

                results.add(String.format(
                        "=== %s (%s) ===\n%s\n",
                        cmd.getName(),
                        cmd.getCommandType(),
                        result.trim()
                ));
            }

            return results;

        } catch (JSchException e) {
            String msg = String.format("Ошибка SSH-подключения к %s:%d → %s", host, port, e.getMessage());
            log.error(msg, e);
            throw new RuntimeException(msg, e);

        } catch (Exception e) {
            String msg = String.format("Ошибка выполнения действия '%s' на %s: %s",
                    action.getName(), host, e.getMessage());
            log.error(msg, e);
            throw new RuntimeException(msg, e);

        } finally {
            if (session != null && session.isConnected()) {
                try {
                    session.disconnect();
                    log.debug("SSH-сессия с {} успешно закрыта", host);
                } catch (Exception ex) {
                    log.warn("Ошибка при закрытии SSH-сессии с {}: {}", host, ex.getMessage());
                }
            }
        }
    }

    // ===================================================================
    // ======================= Вспомогательные методы ====================
    // ===================================================================

    private Session createSshSession(String host, int port, String username, String password) throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(username, host, port > 0 ? port : 22);

        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");   // TODO: в проде использовать known_hosts + ключ

        session.connect(15000); // 15 секунд таймаут
        return session;
    }

    /** Выполнение одной SSH-команды через уже существующую сессию */
    private String executeSshCommand(Session session, String command) throws Exception {
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

    /** Выполнение команды через Fortigate REST API (HTTPS) */
    private String executeHttpsFortigateApi(String host, String apiPath, String username, String password) {
        log.info("Выполняю HTTPS API запрос: {}", apiPath);

        try {
            // TODO: Здесь будет полноценная реализация с авторизацией (токен / API-key)
            String url = "https://" + host + apiPath;

            // Пример заглушки — заменить на реальный вызов с заголовками
            return "HTTPS Fortigate API вызван: " + url +
                    "\n(полная реализация с авторизацией будет добавлена позже)";

        } catch (Exception e) {
            log.error("Ошибка при вызове Fortigate API", e);
            return "HTTPS ошибка: " + e.getMessage();
        }
    }

    /** Локальный обработчик (можно расширять) */
    private String executeLocalHandler(Command command, String vendorKey, Long firewallId) {
        if (vendorKey == null) {
            vendorKey = AppConstants.FORTIGATE;
        }

        LocalCommandHandler handler = localHandlerRegistry.getHandler(vendorKey.toLowerCase(), command.getCommand());

        if (handler == null) {
            log.warn("Не найден обработчик для вендора '{}' и команды '{}'", vendorKey, command.getCommand());
            return "Неизвестная локальная команда: " + command.getCommand();
        }

        try {
            return handler.handle(command, firewallId);
        } catch (Exception e) {
            log.error("Ошибка в обработчике {}/{}", vendorKey, command.getCommand(), e);
            return "Ошибка локального обработчика: " + e.getMessage();
        }
    }

    @Override
    public String getSupportedKey() {
        return AppConstants.FORTIGATE;
    }
}
