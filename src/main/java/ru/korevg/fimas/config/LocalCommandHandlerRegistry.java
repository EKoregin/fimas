package ru.korevg.fimas.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.korevg.fimas.service.strategy.handler.LocalCommandHandler;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class LocalCommandHandlerRegistry {

    // Внешний Map: vendorKey → (commandKey → handler)
    private final Map<String, Map<String, LocalCommandHandler>> handlersByVendor = new ConcurrentHashMap<>();

    public LocalCommandHandlerRegistry(List<LocalCommandHandler> allHandlers) {
        // Группируем все обработчики по вендору
        this.handlersByVendor.putAll(
                allHandlers.stream()
                        .collect(Collectors.groupingBy(
                                LocalCommandHandler::getVendorKey,
                                Collectors.toMap(
                                        LocalCommandHandler::getCommandKey,
                                        handler -> handler,
                                        (existing, replacement) -> replacement, // последний побеждает при конфликте
                                        ConcurrentHashMap::new
                                )
                        ))
        );

        log.info("LocalCommandHandlerRegistry инициализирован. Зарегистрировано {} вендоров:", handlersByVendor.size());
        handlersByVendor.forEach((vendor, map) ->
                log.info("  - {}: {} обработчиков ({})", vendor, map.size(), map.keySet())
        );
    }

    /**
     * Получить обработчик по вендору и ключу команды
     */
    public LocalCommandHandler getHandler(String vendorKey, String commandKey) {
        Map<String, LocalCommandHandler> vendorMap = handlersByVendor.get(vendorKey);
        if (vendorMap == null) {
            log.warn("Не найдена группа обработчиков для вендора: {}", vendorKey);
            return null;
        }
        return vendorMap.get(commandKey);
    }

    /**
     * Получить все обработчики для конкретного вендора
     */
    public Map<String, LocalCommandHandler> getHandlersForVendor(String vendorKey) {
        return handlersByVendor.getOrDefault(vendorKey, Map.of());
    }

    /**
     * Получить все зарегистрированные вендоры
     */
    public Set<String> getSupportedVendors() {
        return handlersByVendor.keySet();
    }
}
