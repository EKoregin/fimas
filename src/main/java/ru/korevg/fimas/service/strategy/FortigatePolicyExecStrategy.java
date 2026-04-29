package ru.korevg.fimas.service.strategy;

import com.jcraft.jsch.JSchException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.korevg.fimas.config.AppConstants;
import ru.korevg.fimas.config.LocalCommandHandlerRegistry;
import ru.korevg.fimas.entity.Action;
import ru.korevg.fimas.entity.Command;
import ru.korevg.fimas.entity.CommandType;
import ru.korevg.fimas.service.strategy.handler.LocalCommandHandler;
import ru.korevg.fimas.util.SshExecutor;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class FortigatePolicyExecStrategy implements PolicyExecStrategy {

    private final RestTemplate restTemplate = new RestTemplate(); // можно заинжектить, если нужно
    private final LocalCommandHandlerRegistry localHandlerRegistry;
    private final SshExecutor sshExecutor;

    public FortigatePolicyExecStrategy(LocalCommandHandlerRegistry localHandlerRegistry, SshExecutor sshExecutor) {
        this.localHandlerRegistry = localHandlerRegistry;
        this.sshExecutor = sshExecutor;
    }

    @Override
    public List<String> execute(Long firewallId, Action action, String vendorKey, String host, int port, String username, String password) {
        List<String> results = new ArrayList<>();

        // Определяем, нужен ли SSH-сессия вообще
        boolean needsSsh = action.getCommands().stream()
                .anyMatch(cmd -> cmd.getCommandType() == CommandType.SSH);

        try {
            if (needsSsh) {
                log.info("Выполнение действия '{}' на хосте {}:{}", action.getName(), host, port);
                sshExecutor.createSshSession(host, port, username, password);
                log.debug("SSH-сессия успешно установлена с {}", host);
            }

            for (Command cmd : action.getCommands()) {
                String result;

                switch (cmd.getCommandType()) {
                    case SSH:
                        result = sshExecutor.executeSshCommand(cmd.getCommand());
                        break;

                    case HTTPS:
                        result = executeHttpsFortigateApi(host, cmd.getCommand(), username, password);
                        break;

                    case LOCAL:
                        result = executeLocalHandler(cmd, vendorKey, firewallId, username, password);
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
            sshExecutor.disconnect();
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
    private String executeLocalHandler(Command command, String vendorKey, Long firewallId, String username, String password) {
        if (vendorKey == null) {
            vendorKey = AppConstants.FORTIGATE;
        }

        LocalCommandHandler handler = localHandlerRegistry.getHandler(vendorKey.toLowerCase(), command.getCommand());

        if (handler == null) {
            log.warn("Не найден обработчик для вендора '{}' и команды '{}'", vendorKey, command.getCommand());
            return "Неизвестная локальная команда: " + command.getCommand();
        }

        try {
            return handler.handle(command, firewallId, username, password);
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
