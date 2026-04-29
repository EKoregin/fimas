package ru.korevg.fimas.service.strategy.handler.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.korevg.fimas.config.AppConstants;
import ru.korevg.fimas.entity.Command;
import ru.korevg.fimas.entity.Port;
import ru.korevg.fimas.entity.Protocol;
import ru.korevg.fimas.entity.Service;
import ru.korevg.fimas.repository.ServiceRepository;
import ru.korevg.fimas.service.strategy.handler.LocalCommandHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Component
@RequiredArgsConstructor
public class FortigateCliServiceGenerate implements LocalCommandHandler {

    private final ServiceRepository serviceRepository;

    @Override
    public String handle(Command command, Long firewallId, String username, String password) {
        log.info("Создание конфигурации CLI для сервисов Fortigate: {}", command.getName());

        List<Service> services = serviceRepository.findAllWithPorts();

        String configBody = services.stream()
                .map(this::serviceToEditBlock)
                .collect(Collectors.joining("\n"));

        log.info("Создание конфигурации завершено");

        return """
                config firewall service custom
                %s
                end
                Всего %s сервисов
                """.formatted(configBody, services.size());
    }

    /**
     * Преобразует один Service в полноценный блок edit ... next
     */
    private String serviceToEditBlock(Service service) {
        if (service.getPorts() == null || service.getPorts().isEmpty()) {
            return """
                    edit "%s"
                        set protocol IP
                    next
                    """.formatted(service.getName());
        }

        // Собираем все TCP и UDP порты отдельно
        List<String> tcpPorts = new ArrayList<>();
        List<String> udpPorts = new ArrayList<>();

        for (Port port : service.getPorts()) {
            String portStr = getPortString(port); // "1883" или "2012-2015"

            if (port.getProtocol() == Protocol.TCP) {
                tcpPorts.add(portStr);
            } else if (port.getProtocol() == Protocol.UDP) {
                udpPorts.add(portStr);
            } else {
                // Для не-TCP/UDP протоколов возвращаем отдельный блок (как было раньше)
                return buildNonTcpUdpBlock(service.getName(), port);
            }
        }

        // Формируем основной блок
        StringBuilder sb = new StringBuilder();
        sb.append("edit \"").append(service.getName()).append("\"\n");

        if (!tcpPorts.isEmpty()) {
            sb.append("    set tcp-portrange ")
                    .append(String.join(" ", tcpPorts))
                    .append("\n");
        }

        if (!udpPorts.isEmpty()) {
            sb.append("    set udp-portrange ")
                    .append(String.join(" ", udpPorts))
                    .append("\n");
        }

        sb.append("next");
        return sb.toString();
    }

    /**
     * Возвращает строковое представление порта (одиночный или диапазон)
     */
    private String getPortString(Port port) {
        String dst = port.getDstPort();
        if (dst == null || dst.isBlank()) {
            return "1-65535"; // fallback
        }
        return dst.trim(); // уже может быть "1883" или "2012-2015"
    }

    /**
     * Для протоколов, которые не TCP и не UDP (ICMP, GRE и т.д.)
     */
    private String buildNonTcpUdpBlock(String serviceName, Port port) {
        String protocolName = switch (port.getProtocol()) {
            case ICMP -> "ICMP";
            case IP -> "IP";
            default -> port.getProtocol().name();
        };

        return """
                edit "%s"
                    set protocol %s
                next
                """.formatted(serviceName, protocolName);
    }

    @Override
    public String getCommandKey() {
        return "config firewall service custom";
    }

    @Override
    public String getVendorKey() {
        return AppConstants.FORTIGATE;
    }
}
