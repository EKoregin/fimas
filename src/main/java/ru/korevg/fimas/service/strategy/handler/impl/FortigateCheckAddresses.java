package ru.korevg.fimas.service.strategy.handler.impl;


import com.jcraft.jsch.JSchException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.korevg.fimas.config.AppConstants;
import ru.korevg.fimas.dto.firewall.FirewallResponse;
import ru.korevg.fimas.entity.Address;
import ru.korevg.fimas.entity.Command;
import ru.korevg.fimas.entity.Policy;
import ru.korevg.fimas.exception.EntityNotFoundException;
import ru.korevg.fimas.service.AddressService;
import ru.korevg.fimas.service.FirewallService;
import ru.korevg.fimas.service.strategy.handler.LocalCommandHandler;
import ru.korevg.fimas.util.SshExecutor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class FortigateCheckAddresses implements LocalCommandHandler {

    private final FirewallService firewallService;
    private final AddressService addressService;
    private final SshExecutor sshExecutor;

    private static final String GET_ADDRESS_COMMAND = "show firewall addrgrp";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    private static final Pattern NAME_GROUP_PATTERN = Pattern.compile("edit \"([^\"]+)\"");


    @Override
    public String handle(Command command, Long firewallId, String username, String password) {
        FirewallResponse firewall = firewallService.findById(firewallId)
                .orElseThrow(() -> new EntityNotFoundException("Firewall с ID: " + firewallId + " не найден"));

        log.info("Проверка адрессных групп на Firewall: {}", firewall.name());

        String rawAddrGroups = getAddressFromRemoteFW(firewall.mgmtIpAddress(), username, password);

//        log.info(rawAddrGroups);
        
        Map<String, List<String>> remoteAddrGroups = parseAddrGroups(rawAddrGroups);
        List<Address> localAddresses = addressService.findAllByFirewallId(firewallId);

        return buildComparisonHtml(localAddresses, remoteAddrGroups, firewall.name());
    }

    /**
     * Основной метод построения HTML-отчёта
     */
    private String buildComparisonHtml(List<Address> localAddresses,
                                       Map<String, List<String>> remoteAddrGroups,
                                       String firewallName) {

        StringBuilder sb = new StringBuilder();

        AtomicInteger counter = new AtomicInteger(1);
        AtomicInteger extraCounter = new AtomicInteger(0);

        sb.append("<div style='font-family: Arial, sans-serif; padding: 20px;'>");
        sb.append("<style>");
        sb.append("  table { border-collapse: collapse; width: 100%; }");
        sb.append("  th, td { border: 1px solid #ccc; padding: 10px; }");
        sb.append("</style>");
        sb.append("<h2>Сравнение адресов: ").append(firewallName).append("</h2>");
        sb.append("<p><strong>Дата проверки:</strong> ")
                .append(LocalDateTime.now().format(DATE_TIME_FORMATTER))
                .append("</p>");

        // Таблица локальных адресов
        appendLocalAddressesTable(sb, localAddresses, remoteAddrGroups, counter);

        // Таблица адресов, которых нет в базе но есть на Firewall
        appendExtraAddressesTable(sb, localAddresses, remoteAddrGroups, extraCounter);

        sb.append("<h3>Итог:</h3>");
        sb.append("<p>Всего дополнительных адресных групп на FW: <strong>")
                .append(extraCounter.get()).append("</strong></p>");

        sb.append("</div>");

        return sb.toString();
    }



    private void appendLocalAddressesTable(StringBuilder sb, List<Address> localAddrGroups, Map<String, List<String>> remoteAddrGroups, AtomicInteger counter) {
        sb.append("<h3>Адреса из базы</h3>");
        sb.append("<table style='border-collapse: collapse; width: 100%;'>");
        sb.append("<tr style='background-color: #f0f0f0;'>")
                .append("<th>№</th>")
                .append("<th>Адресная группа</th>")
                .append("<th>Статус</th>")
                .append("</tr>");

        for (Address localAddressGroup : localAddrGroups) {
            String localAddrGroupName = localAddressGroup.getName();

            sb.append("<tr>")
                    .append("<td>").append(counter.getAndIncrement()).append("</td>")
                    .append("<td>").append(localAddrGroupName).append("</td>");

            if (remoteAddrGroups.containsKey(localAddrGroupName)) {
                List<String> remoteAddresses = remoteAddrGroups.get(localAddrGroupName);
                Set<String> localAddresses = localAddressGroup.getAddresses();
                sb.append("<tr>")
                        .append("<td></td>")
                        .append("<td>Создана на FW</td>");

                List<String> onlyInLocal = localAddresses.stream()
                        .filter(addr -> !remoteAddresses.contains(addr))
                        .toList();

                List<String> onlyInRemote = remoteAddresses.stream()
                        .filter(addr -> !localAddresses.contains(addr))
                        .toList();
                // Если адреса совпадают в группах, то вывести сообщение, что группа уже создана на FW
                if (onlyInLocal.isEmpty() && onlyInRemote.isEmpty()) {
                    sb.append("<td style='color: green;'>Группы совпадают</td>");
//                    sb.append("<tr>")
//                            .append("<td></td>")
//                            .append("<td>Local: <br>Remote: ")
//                            .append("</td>")
//                            .append("<td>")
//                            .append("Local:&nbsp;&nbsp;").append(localAddressGroup.getAddresses()).append("<br>")
//                            .append("Remote: ").append(remoteAddresses)
//                            .append("</td>")
//                            .append("</tr>");
                } else {
                    sb.append("<td style='color: red;'>Не совпадают</td>");
                    sb.append("<tr>")
                            .append("<td></td>")
                            .append("<td style='text-align: right; font-weight: bold;'>")
                            .append(" — Local: <br>Remote: ")
                            .append("</td>")
                            .append("<td>")
                            .append("Только в Local: ").append(onlyInLocal).append("<br>")
                            .append("Только в Remote: ").append(onlyInRemote)
                            .append("</td>")
                            .append("</tr>");
                }
            } else {
                sb.append("<td style='color: red;'>Группа не создана на удаленном FW</td>").append("</tr>");
            }
        }
        sb.append("</table>");
    }

    private void appendExtraAddressesTable(StringBuilder sb,
                                           List<Address> localAddresses,
                                           Map<String, List<String>> remoteAddrGroups,
                                           AtomicInteger extraCounter) {
        Set<String> localGroupNames = localAddresses.stream().map(Address::getName).collect(Collectors.toSet());

        sb.append("<h3>Адресные группы, которых нет в базе, но есть на Firewall</h3>");
        sb.append("<table style='border-collapse: collapse; width: 100%;'>");
        sb.append("<tr style='background-color: #f0f0f0;'><th>№</th><th>Адрес</th></tr>");

        remoteAddrGroups.forEach((name, members) -> {
           if (!localGroupNames.contains(name)) {
               sb.append("<tr>")
                       .append("<td>").append(extraCounter.getAndIncrement()).append("</td>")
                       .append("<td>").append(name).append("</td>")
                       .append("</tr>");
           }
        });

        sb.append("</table>");
    }

    private Map<String, List<String>> parseAddrGroups(String rawAddrGroups) {
        Map<String, List<String>> result = new HashMap<>();
        String[] blocks = rawAddrGroups.split("(?=edit\\s)");
        for (String block : blocks) {
            if (block.trim().isEmpty()) continue;
            
            String addrGroupName = extractGroupName(block);
            if (addrGroupName == null) continue;
            
            List<String> groupMembers = extractGroupMembers(block);
            if (groupMembers.isEmpty()) {
                log.warn("Group: {} is empty", addrGroupName);
            }

            if (addrGroupName.equals("DC-Networks")) {
                log.info(groupMembers.toString());
            }

            result.put(addrGroupName, groupMembers);
        }
        
        return result;
    }

    private String extractGroupName(String block) {
        Matcher matcher = NAME_GROUP_PATTERN.matcher(block);
        return matcher.find() ? matcher.group(1) : null;
    }

    private List<String> extractGroupMembers(String block) {
        if (block == null || block.isBlank()) {
            return List.of();
        }

        return Arrays.stream(block.split("\\n"))
                .map(String::trim)
                .filter(line -> line.startsWith("set member "))
                .flatMap(line -> {
                    String content = line.substring("set member ".length()).trim();
                    return Arrays.stream(content.split("\"\\s*\""))
                            .map(token -> token.replace("\"", "").trim())
                            .filter(s -> !s.isEmpty());
                })
                .toList();
    }

    /**
     * Получение адресов с удалённого Fortigate по SSH
     */
    private String getAddressFromRemoteFW(String ipAddress, String username, String password) {
        try {
            sshExecutor.createSshSession(ipAddress, AppConstants.SSH, username, password);
            return sshExecutor.executeSshCommand(GET_ADDRESS_COMMAND);
        } catch (JSchException e) {
            log.error("Ошибка SSH-подключения к {}: {}", ipAddress, e.getMessage(), e);
            throw new RuntimeException("Не удалось подключиться к Firewall " + ipAddress, e);
        } catch (Exception e) {
            log.error("Ошибка выполнения команды '{}' на {}: {}", GET_ADDRESS_COMMAND, ipAddress, e.getMessage(), e);
            throw new RuntimeException("Ошибка получения адресов с Firewall", e);
        } finally {
            sshExecutor.disconnect();
        }
    }

    @Override
    public String getCommandKey() {
        return "check addresses";
    }

    @Override
    public String getVendorKey() {
        return AppConstants.FORTIGATE;
    }
}
