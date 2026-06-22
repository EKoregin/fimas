package ru.korevg.fimas.service.strategy.handler.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.korevg.fimas.entity.Address;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static ru.korevg.fimas.config.AppConstants.DATE_TIME_FORMATTER;

@Slf4j
@Service
public class HtmlReportAddressService {

    /**
     * Основной метод построения HTML-отчёта
     */
    public String buildComparisonHtml(List<Address> localAddresses,
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

            if (remoteAddrGroups.containsKey(localAddrGroupName)) {
                List<String> remoteAddresses = remoteAddrGroups.get(localAddrGroupName);
                Set<String> localAddresses = localAddressGroup.getAddresses();

                List<String> onlyInLocal = localAddresses.stream()
                        .filter(addr -> !remoteAddresses.contains(addr))
                        .toList();

                List<String> onlyInRemote = remoteAddresses.stream()
                        .filter(addr -> !localAddresses.contains(addr))
                        .toList();

                if (!onlyInLocal.isEmpty() || !onlyInRemote.isEmpty()) {
                    sb.append("<tr>")
                            .append("<td>").append(counter.getAndIncrement()).append("</td>")
                            .append("<td class=\"no-wrap-column\" style='text-align: right; font-weight: bold;'>")
                            .append(localAddrGroupName)
                            .append("</td>");
                    sb.append("<td style='color: red;'>Есть отличия</td>");
                    sb.append("<tr>")
                            .append("<td></td>")
                            .append("<td style='text-align: right;'>")
                            .append("Local: <br>Remote: ")
                            .append("</td>")
                            .append("<td>")
                            .append("Только в Local: ").append(onlyInLocal).append("<br>")
                            .append("Только в Remote: ").append(onlyInRemote)
                            .append("</td>")
                            .append("</tr>");
                }
            } else {
                sb.append("<tr>")
                        .append("<td>").append(counter.getAndIncrement()).append("</td>")
                        .append("<td class=\"no-wrap-column\" style='text-align: right; font-weight: bold;'>")
                        .append(localAddrGroupName)
                        .append("</td>");
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
}
