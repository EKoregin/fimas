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
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class FortigateCliAddressesGenerate implements LocalCommandHandler {

    private final PolicyService policyService;
    private final AddressService addressService;

    /**
     * Флаг: true = пропускать IPv6 адреса (рекомендуется для FortiGate CLI в большинстве случаев)
     *       false = обрабатывать IPv6 (если потребуется в будущем)
     */
    private static final boolean SKIP_IPV6 = true;   // ← Здесь можно переключить

    @Override
    public String handle(Command command, Long firewallId) {
        log.info("Создание конфигурации CLI для адресов и addrgrp Fortigate: {}", command.getName());

        List<Address> addresses = addressService.findAllByFirewallId(firewallId);

        if (addresses.isEmpty()) {
            log.warn("Для firewallId={} не найдено ни одного адреса", firewallId);
            return "# Нет адресов для данного firewall\n";
        }

        StringBuilder cli = new StringBuilder();

        cli.append("config firewall address\n");
        cli.append(buildAddressBlocks(addresses));
        cli.append("end\n\n");

        cli.append("config firewall addrgrp\n");
        cli.append(buildAddrgrpBlocks(addresses));
        cli.append("end\n");

        log.info("Создание конфигурации адресов Fortigate завершено ({} адресов)", addresses.size());
        return cli.toString();
    }

    /**
     * Генерирует блоки edit для отдельных адресов с поддержкой префиксов и пропуском IPv6
     */
    private String buildAddressBlocks(List<Address> addresses) {
        StringBuilder sb = new StringBuilder();

        for (Address addr : addresses) {
            for (String inet : addr.getAddresses()) {
                String value = inet.trim();
                if (value.isEmpty()) continue;

                // === НОВАЯ ЛОГИКА: Пропуск IPv6 ===
                if (SKIP_IPV6 && isIPv6(value)) {
                    log.debug("Пропущен IPv6 адрес: {}", value);
                    continue;
                }

                // Разделяем адрес и префикс (например "10.20.30.0/24" или "2001:db8::/32")
                String ipPart;
                int prefix = 32; // по умолчанию — хост (/32)

                if (value.contains("/")) {
                    String[] parts = value.split("/");
                    ipPart = parts[0].trim();
                    try {
                        prefix = Integer.parseInt(parts[1].trim());
                        if (prefix < 0 || prefix > 128) prefix = 32; // 128 — максимум для IPv6
                    } catch (Exception e) {
                        log.warn("Некорректный префикс в адресе '{}', используется /32", value);
                        prefix = 32;
                    }
                } else {
                    ipPart = value;
                }

                String subnetMask = prefixToSubnetMask(prefix);

                sb.append("    edit \"").append(value).append("\"\n");

                if (addr.getSubType() == AddressSubType.IP) {
                    if (isIPv6(value)) {
                        // Для IPv6 в FortiGate обычно используется set ip6
                        sb.append("        set ip6 ").append(value).append("\n");
                    } else {
                        sb.append("        set subnet ").append(ipPart).append(" ").append(subnetMask).append("\n");
                    }
                }
                else if (addr.getSubType() == AddressSubType.FQDN) {
                    sb.append("        set type fqdn\n");
                    sb.append("        set fqdn \"").append(value).append("\"\n");
                }
                else {
                    // fallback
                    sb.append("        set subnet ").append(ipPart).append(" ").append(subnetMask).append("\n");
                }

                sb.append("    next\n");
            }
        }
        return sb.toString();
    }

    /**
     * Простая проверка, является ли строка IPv6 адресом
     */
    private boolean isIPv6(String address) {
        if (address == null) return false;
        String trimmed = address.trim();
        return trimmed.contains(":") && !trimmed.contains("."); // грубая, но надёжная эвристика
    }

    /**
     * Преобразует CIDR prefix в subnet mask (только для IPv4)
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

    /**
     * Генерирует блоки для addrgrp (без изменений)
     */
    private String buildAddrgrpBlocks(List<Address> addresses) {
        StringBuilder sb = new StringBuilder();

        for (Address addr : addresses) {
            String groupName = addr.getName().trim();
            if (groupName.isEmpty()) continue;

            // Фильтруем IPv6 из группы, если включён пропуск
            Set<String> members = addr.getAddresses().stream()
                    .filter(m -> !(SKIP_IPV6 && isIPv6(m)))
                    .collect(Collectors.toSet());

            if (members.isEmpty()) continue;

            sb.append("    edit \"").append(groupName).append("\"\n");
            sb.append("        set member ");

            String membersStr = members.stream()
                    .map(m -> "\"" + m.trim() + "\"")
                    .collect(Collectors.joining(" "));

            sb.append(membersStr).append("\n");
            sb.append("    next\n");
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

//package ru.korevg.fimas.service.strategy.handler.impl;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//import ru.korevg.fimas.config.AppConstants;
//import ru.korevg.fimas.dto.address.AddressShortResponse;
//import ru.korevg.fimas.dto.policy.PolicyResponse;
//import ru.korevg.fimas.dto.service.ServiceShortResponse;
//import ru.korevg.fimas.entity.Address;
//import ru.korevg.fimas.entity.Command;
//import ru.korevg.fimas.entity.PolicyAction;
//import ru.korevg.fimas.service.AddressService;
//import ru.korevg.fimas.service.PolicyService;
//import ru.korevg.fimas.service.strategy.handler.LocalCommandHandler;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class FortigateCliAddressesGenerate implements LocalCommandHandler {
//
//    private final PolicyService policyService;
//    private final AddressService addressService;
//
//
//    @Override
//    public String handle(Command command, Long firewallId) {
//        log.info("Создание конфигурации CLI для адресов и их груп Fortigate: {}", command.getName());
//        //1. Получаем адреса
//        List<Address> addresses = addressService.findAllByFirewallId(firewallId);
//
//        //3. Создаем блок конфигурации для адресов вида
//        // Каждый адрес содержит множество адресов.
//        // Поле private Set<String> addresses = new HashSet<>();
//        // Для каждого элемента этого множества создаем отдельный объект - address
////        Для адресов у которых subType = AddressSubType.IP, блоки вида
////        config firewall address
////        edit "10.20.188.138"
////        set subnet 10.20.188.138 255.255.255.255
////        next
//
////        Для адресов у которых subType = AddressSubType.FQDN
////        edit "gotomypc.com"
////        set type fqdn
////        set fqdn "*.gotomypc.com"
////        next
////        end
//
//        //4. Создаем группы адресов
//        // У каждого адреса есть имя, например vl440-wifi-guest. Группа называется этим именем.
//        // Необходим создать группу адресов для каждого объекта Address и добавить в поле member, созданные перед этим объекты
////        config firewall addrgrp
////        edit "wh-mgmt-group-new"
////        set member "10.180.246.0/24" "10.178.246.0/24" "10.176.246.0/24"
////        next
////        edit "vl440-wifi-guest"
////        set member "11.32.240.0/21" "11.31.240.0/21"
////        next
////        end
//
//        log.info("Создание конфигурации завершено");
//        return "";
//    }
//
//    private String addressToEditBlock(PolicyResponse policy) {
//        StringBuilder sb = new StringBuilder();
//        return sb.toString();
//    }
//
//    private String groupToEditBlock(PolicyResponse policy) {
//        StringBuilder sb = new StringBuilder();
//        return sb.toString();
//    }
//
//
//
//    @Override
//    public String getCommandKey() {
//        return "config firewall address";
//    }
//
//    @Override
//    public String getVendorKey() {
//        return AppConstants.FORTIGATE;
//    }
//}
