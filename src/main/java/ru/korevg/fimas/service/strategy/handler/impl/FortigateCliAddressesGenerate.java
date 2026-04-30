package ru.korevg.fimas.service.strategy.handler.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.korevg.fimas.config.AppConstants;
import ru.korevg.fimas.entity.Address;
import ru.korevg.fimas.entity.AddressSubType;
import ru.korevg.fimas.entity.Command;
import ru.korevg.fimas.service.AddressService;
import ru.korevg.fimas.service.PolicyService;
import ru.korevg.fimas.service.strategy.handler.LocalCommandHandler;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class FortigateCliAddressesGenerate implements LocalCommandHandler {

    private final AddressService addressService;

    /**
     * Флаг: true = пропускать IPv6 адреса (рекомендуется)
     */
    private static final boolean SKIP_IPV6 = true;

    @Override
    public String handle(Command command, Long firewallId, String username, String password) {
        log.info("Создание конфигурации CLI для адресов и addrgrp Fortigate: {}", command.getName());

        List<Address> addresses = addressService.findAllByFirewallId(firewallId);

        if (addresses.isEmpty()) {
            log.warn("Для firewallId={} не найдено ни одного адреса", firewallId);
            return "<h3>Нет адресов для данного firewall</h3>";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<div>");
        sb.append("<h1>config firewall address</h1><p></p>");
        sb.append(buildAddressBlocks(addresses));
        sb.append("end<br><br>");

        sb.append("config firewall addrgrp<br>");
        sb.append(buildAddrgrpBlocks(addresses));
        sb.append("end<br>");

        sb.append("<p>Всего адресов: ")
                .append(addresses.size()).append("</p>");
        sb.append("<p>Из них статических: ")
                .append(addresses.stream().filter(a -> Objects.equals(a.getAddressType(), "COMMON")).count())
                .append("</p>");
        sb.append("<p>Из них динамических: ")
                .append(addresses.stream().filter(a -> Objects.equals(a.getAddressType(), "DYNAMIC")).count())
                .append("</p>");

        sb.append("</div>");
        log.info("Создание конфигурации адресов Fortigate завершено ({} адресов)", addresses.size());
        return sb.toString();
    }


    /**
     * Генерирует блоки edit для адресов с поддержкой:
     * - subnet (IP/подсеть)
     * - iprange (диапазон через "-")
     * - fqdn
     */
    private String buildAddressBlocks(List<Address> addresses) {
        StringBuilder sb = new StringBuilder();

        for (Address addr : addresses) {
            for (String inet : addr.getAddresses()) {
                String value = inet.trim();
                if (value.isEmpty()) continue;

                // Пропуск IPv6
                if (SKIP_IPV6 && isIPv6(value)) {
                    log.debug("Пропущен IPv6 адрес: {}", value);
                    continue;
                }

                String editName = value; // имя объекта = оригинальная строка

                sb.append("    edit \"").append(editName).append("\"<br>");

                if (isIPRange(value) && addr.getSubType() == AddressSubType.IP) {
                    String[] parts = value.split("-");
                    String startIp = parts[0].trim();
                    String endIp = parts[1].trim();

                    sb.append("        set type iprange<br>");
                    sb.append("        set start-ip ").append(startIp).append("<br>");
                    sb.append("        set end-ip ").append(endIp).append("<br>");

                } else if (addr.getSubType() == AddressSubType.IP) {
                    // === Обычный IP или подсеть (subnet) ===
                    if (isIPv6(value)) {
                        sb.append("        set ip6 ").append(value).append("<br>");
                    } else {
                        String ipPart = value.contains("/") ? value.split("/")[0].trim() : value;
                        String subnetMask = prefixToSubnetMask(extractPrefix(value));
                        sb.append("        set subnet ").append(ipPart).append(" ").append(subnetMask).append("<br>");
                    }
                } else if (addr.getSubType() == AddressSubType.FQDN) {
                    sb.append("        set type fqdn<br>");
                    sb.append("        set fqdn \"").append(value).append("\"<br>");
                } else {
                    // fallback
                    String ipPart = value.contains("/") ? value.split("/")[0].trim() : value;
                    String subnetMask = prefixToSubnetMask(extractPrefix(value));
                    sb.append("        set subnet ").append(ipPart).append(" ").append(subnetMask).append("<br>");
                }

                sb.append("    next<br>");
            }
        }
        return sb.toString();
    }

    /**
     * Проверяет, является ли строка диапазоном вида "start-end"
     */
    private boolean isIPRange(String value) {
        if (value == null) return false;
        return value.contains("-") && value.split("-").length == 2;
    }

    /**
     * Простая проверка IPv6
     */
    private boolean isIPv6(String address) {
        if (address == null) return false;
        String trimmed = address.trim();
        return trimmed.contains(":") && !trimmed.contains(".");
    }

    /**
     * Извлекает префикс из строки (по умолчанию 32)
     */
    private int extractPrefix(String value) {
        if (!value.contains("/")) return 32;

        try {
            String[] parts = value.split("/");
            int prefix = Integer.parseInt(parts[1].trim());
            return (prefix < 0 || prefix > 128) ? 32 : prefix;
        } catch (Exception e) {
            log.warn("Некорректный префикс в адресе '{}', используется /32", value);
            return 32;
        }
    }

    /**
     * Преобразует CIDR prefix в subnet mask (только IPv4)
     */
    private String prefixToSubnetMask(int prefix) {
        if (prefix <= 0) return "0.0.0.0";
        if (prefix >= 32) return "255.255.255.255";

        long mask = 0xFFFFFFFFL << (32 - prefix);
        return String.format("%d.%d.%d.%d",
                (mask >> 24) & 0xFF,
                (mask >> 16) & 0xFF,
                (mask >> 8) & 0xFF,
                mask & 0xFF);
    }

    private String buildAddrgrpBlocks(List<Address> addresses) {
        StringBuilder sb = new StringBuilder();

        for (Address addr : addresses) {
            String groupName = addr.getName().trim();
            if (groupName.isEmpty()) continue;

            // Фильтруем IPv6
            Set<String> members = addr.getAddresses().stream()
                    .filter(m -> !(SKIP_IPV6 && isIPv6(m)))
                    .collect(Collectors.toSet());

            if (members.isEmpty()) continue;

            sb.append("    edit \"").append(groupName).append("\"<br>");
            sb.append("        set member ");

            String membersStr = members.stream()
                    .map(m -> "\"" + m.trim() + "\"")
                    .collect(Collectors.joining(" "));

            sb.append(membersStr).append("<br>");
            sb.append("    next<br>");
        }
        return sb.toString();
    }

    @Override
    public String getCommandKey() {
        return "config firewall address";
    }

    @Override
    public String getVendorKey() {
        return AppConstants.FORTIGATE;
    }
}
