package ru.korevg.fimas.service.strategy.handler.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.korevg.fimas.config.AppConstants;
import ru.korevg.fimas.entity.Address;
import ru.korevg.fimas.entity.Policy;
import ru.korevg.fimas.entity.PolicyAction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static ru.korevg.fimas.config.AppConstants.DATE_TIME_FORMATTER;

@Slf4j
@Service
public class HtmlReportPoliciesService {

    /**
     * Основной метод построения HTML-отчёта
     */
    public String buildComparisonHtml(List<Policy> localPolicies,
                                      Map<String, Map<String, List<String>>> remotePolicies,
                                      String firewallName, String vendor) {

        StringBuilder sb = new StringBuilder();

        AtomicInteger counter = new AtomicInteger(1);
        AtomicInteger problemCounter = new AtomicInteger(0);
        AtomicInteger extraEnabled = new AtomicInteger(0);
        AtomicInteger extraDisabled = new AtomicInteger(0);

        sb.append("<div style='font-family: Arial, sans-serif; padding: 20px;'>");
        sb.append("<style>");
        sb.append("  table { border-collapse: collapse; width: 100%; }");
        sb.append("  th, td { border: 1px solid #ccc; padding: 10px; }");
        sb.append("</style>");
        sb.append("<h2>Сравнение политик: ").append(firewallName).append("</h2>");
        sb.append("<p><strong>Дата проверки:</strong> ")
                .append(LocalDateTime.now().format(DATE_TIME_FORMATTER))
                .append("</p>");

        // Таблица локальных политик
        appendLocalPoliciesTable(sb, localPolicies, remotePolicies, counter, problemCounter, vendor);

        sb.append("<h4>Всего проблем: ").append(problemCounter.get()).append("</h4>");

        // Таблица политик, которых нет в базе
        appendExtraPoliciesTable(sb, localPolicies, remotePolicies, counter, extraEnabled, extraDisabled);

        sb.append("<h3>Итог:</h3>");
        sb.append("<p>Всего дополнительных политик на FW: <strong>")
                .append(extraEnabled.get() + extraDisabled.get()).append("</strong></p>");
        sb.append("<p>Из них включённых: <strong>").append(extraEnabled.get()).append("</strong></p>");
        sb.append("<p>Из них выключенных: <strong>").append(extraDisabled.get()).append("</strong></p>");

        sb.append("</div>");

        return sb.toString();
    }

    private void appendLocalPoliciesTable(StringBuilder sb,
                                          List<Policy> localPolicies,
                                          Map<String, Map<String, List<String>>> remotePolicies,
                                          AtomicInteger counter, AtomicInteger problemCounter, String vendor) {

        sb.append("<h3>Политики из базы</h3>");
        sb.append("<table style='border-collapse: collapse; width: 100%;'>");
        sb.append("<tr style='background-color: #f0f0f0;'>")
                .append("<th>№</th>")
                .append("<th>Правило</th>")
                .append("<th style='text-align: left';>Статус на FW</th>")
                .append("</tr>");

        for (Policy policy : localPolicies) {
            RemotePolicyData remote = getRemotePolicyData(remotePolicies, policy.getName(), vendor);

            String status = remote != null ? remote.status() : "Отсутствует на FW";
            String statusStyle = remote != null ? "color: green;" : "color: red;";

            if (remote != null && remote.status().equals("disable")) {
                statusStyle = "color: orange;";
            }

            // Основная строка политики
            sb.append("<tr>")
                    .append("<td>").append(counter.getAndIncrement()).append("</td>")
                    .append("<td class=\"no-wrap-column\" style='text-align: right; font-weight: bold;'>")
                    .append(policy.getName())
                    .append("</td>")
                    .append("<td style='").append(statusStyle).append("'>").append(status).append("</td>")
                    .append("</tr>");

            // Детальное сравнение (только если политика есть на удалённом FW)
            if (remote != null) {
                appendPolicyComparisonRows(sb, policy, remote, problemCounter, vendor);
            }
        }

        sb.append("</table>");
    }

    private record RemotePolicyData(
            String status,
            String srcZone,
            String dstZone,
            String action,
            List<String> srcAddr,
            List<String> dstAddr,
            List<String> services
    ) {
    }

    private record VendorMapping(
            String srcZoneKey,
            String dstZoneKey,
            String srcAddrKey,
            String dstAddrKey) {

        public static VendorMapping forVendor(String vendor) {
            if (vendor == null) {
                return DEFAULT;
            }

            return switch (vendor.toLowerCase().trim()) {
                case "fortinet", "fortigate" -> FORTINET;
                case "paloalto", "pan", "panos" -> PALO_ALTO;
                case "checkpoint", "cp" -> CHECKPOINT;
                case "huawei" -> HUAWEI;
                default -> DEFAULT;
            };
        }

        private static final VendorMapping FORTINET =
                new VendorMapping("srcintf", "dstintf", "srcaddr", "dstaddr");

        private static final VendorMapping PALO_ALTO =
                new VendorMapping("source-zone", "destination-zone", "source-address", "destination-address");

        private static final VendorMapping CHECKPOINT =
                new VendorMapping("source-zone", "destination-zone", "source-address", "destination-address");

        private static final VendorMapping HUAWEI =
                new VendorMapping("source-zone", "destination-zone", "source-address", "destination-address");

        private static final VendorMapping DEFAULT =
                new VendorMapping("srcintf", "dstintf", "srcaddr", "dstaddr");
    }

    private RemotePolicyData getRemotePolicyData(Map<String, Map<String, List<String>>> remotePolicies, String policyName, String vendor) {
        Map<String, List<String>> policy = remotePolicies.get(policyName);
        if (policy == null) {
            return null;
        }

        VendorMapping mapping = VendorMapping.forVendor(vendor);

        return new RemotePolicyData(
                getFirstOrDefault(policy, "status", "Enabled"),
                getFirstOrEmpty(policy, mapping.srcZoneKey()),
                getFirstOrEmpty(policy, mapping.dstZoneKey()),
                getFirstOrDefault(policy, "action", "deny"),
                policy.getOrDefault(mapping.srcAddrKey(), List.of()),
                policy.getOrDefault(mapping.dstAddrKey(), List.of()),
                policy.getOrDefault("service", List.of())
        );
    }

    private String getFirstOrDefault(Map<String, List<String>> map, String key, String defaultValue) {
        List<String> list = map.getOrDefault(key, List.of());
        return list.isEmpty() ? defaultValue : list.getFirst();
    }

    private String getFirstOrEmpty(Map<String, List<String>> map, String key) {
        List<String> list = map.getOrDefault(key, List.of());
        return list.isEmpty() ? "" : list.getFirst();
    }

    // Вывод детального сравнения (зоны, action, адреса)
    private void appendPolicyComparisonRows(StringBuilder sb,
                                            Policy policy,
                                            RemotePolicyData remote,
                                            AtomicInteger counter,
                                            String vendor) {

        boolean isSrcZoneMatch;
        boolean isDstZoneMatch;

        // Для Huawei зоны не проверяются.
        if (vendor.equals(AppConstants.HUAWEI)) {
            isSrcZoneMatch = true;
            isDstZoneMatch = true;
        } else {
            isSrcZoneMatch = policy.getSrcZone().getName().equals(remote.srcZone());
            isDstZoneMatch = policy.getDstZone().getName().equals(remote.dstZone());
        }

        // Source Zone
        appendComparisonRow(sb, counter, "Source Zone",
                "Local: " + policy.getSrcZone().getName(),
                "Remote: " + remote.srcZone(),
                isSrcZoneMatch);

        // Destination Zone
        appendComparisonRow(sb, counter, "Destination Zone",
                "Local: " + policy.getDstZone().getName(),
                "Remote: " + remote.dstZone(),
                isDstZoneMatch);

        // Action
        String localAction = policy.getAction() == PolicyAction.PERMIT ? "permit" : "deny";
        String remoteAction = normalizeRemoteAction(remote.action());
        boolean matchAction = localAction.equals(remoteAction);

        appendComparisonRow(sb, counter, "Action",
                "Local: " + localAction,
                "Remote: " + remote.action(),
                matchAction);

        // Source Addresses (только различия)
        appendAddressComparisonRow(sb, counter, "Source Addresses",
                policy.getSrcAddresses().stream().map(Address::getName).toList(),
                remote.srcAddr(),
                vendor);

        // Destination Addresses (только различия)
        appendAddressComparisonRow(sb, counter, "Destination Addresses",
                policy.getDstAddresses().stream().map(Address::getName).toList(),
                remote.dstAddr(),
                vendor);

        // Services (только различия)
        appendAddressComparisonRow(sb, counter, "Services",
                policy.getServices().stream().map(ru.korevg.fimas.entity.Service::getName).toList(),
                remote.services,
                vendor);
    }

    private String normalizeRemoteAction(String action) {
        if (action == null) {
            return "deny";
        }
        String lower = action.toLowerCase().trim();
        return switch (lower) {
            case "permit", "accept" -> "permit";
            default -> "deny";
        };
    }

    private void appendComparisonRow(StringBuilder sb,
                                     AtomicInteger problemCounter,
                                     String label,
                                     String localValue,
                                     String remoteValue,
                                     boolean isMatch) {
        if (isMatch) {
            return;
        }

        problemCounter.getAndIncrement();

        sb.append("<tr>")
                .append("<td></td>")
                .append("<td style='text-align: right;'>")
                .append(label)
                .append("</td>")
                .append("<td style='color: red;'>").append(localValue).append(" | ").append(remoteValue)
                .append("</td>")
                .append("</tr>");
    }

    private void appendAddressComparisonRow(StringBuilder sb,
                                            AtomicInteger problemCounter,
                                            String label,
                                            List<String> localAddrs,
                                            List<String> remoteAddrs,
                                            String vendor) {

        // Добавляем any, если адресов нет
        if (localAddrs.isEmpty()) {
            if (vendor.equals(AppConstants.HUAWEI)) {
                localAddrs = List.of("any");
            } else if (vendor.equals(AppConstants.FORTIGATE)) {
                localAddrs = List.of("all");
            }
        }

        List<String> onlyInLocal = localAddrs.stream()
                .filter(addr -> !remoteAddrs.contains(addr))
                .toList();

        List<String> finalLocalAddrs = localAddrs;
        List<String> onlyInRemote = remoteAddrs.stream()
                .filter(addr -> !finalLocalAddrs.contains(addr))
                .toList();



        if (onlyInLocal.isEmpty() && onlyInRemote.isEmpty()) {
            return;
        } else {
            problemCounter.getAndIncrement();
            sb.append("<tr>")
                    .append("<td></td>")
                    .append("<td style='text-align: right;'>")
                    .append(label)
                    .append("</td>")
                    .append("<td>");
            if (!onlyInLocal.isEmpty()) {
                sb.append("Только в шаблоне: ")
                        .append("<span style='color: orange;'>")
                        .append(onlyInLocal)
                        .append("</span>")
                        .append("<br>");
            }
            if (!onlyInRemote.isEmpty()) {
                sb.append("Только на удаленном FW: ")
                        .append("<span style='color: orange;'>")
                        .append(onlyInRemote)
                        .append("</span>")
                        .append("<br>");
            }
            sb.append("</td>").append("</tr>");

        }
    }

    private void appendExtraPoliciesTable(StringBuilder sb,
                                          List<Policy> localPolicies,
                                          Map<String, Map<String, List<String>>> remotePolicies,
                                          AtomicInteger counter,
                                          AtomicInteger extraEnabled,
                                          AtomicInteger extraDisabled) {

        Set<String> localNames = localPolicies.stream()
                .map(Policy::getName)
                .collect(Collectors.toSet());

        sb.append("<br><h3>Политики, которых нет в базе, но есть на удаленном Firewall</h3>");
        sb.append("<table style='border-collapse: collapse; width: 100%;'>");
        sb.append("<tr style='background-color: #f0f0f0;'><th>№</th><th>Правило</th><th>Статус</th></tr>");

        remotePolicies.forEach((name, params) -> {
            if (!localNames.contains(name)) {
                String status = params.getOrDefault("status", List.of("Enabled")).getFirst();
                boolean isEnabled = "enabled".equalsIgnoreCase(status);

                if (isEnabled) extraEnabled.incrementAndGet();
                else extraDisabled.incrementAndGet();

                sb.append("<tr>")
                        .append("<td>").append(counter.getAndIncrement()).append("</td>")
                        .append("<td>").append(name).append("</td>")
                        .append("<td>").append(status).append("</td>")
                        .append("</tr>");
            }
        });

        sb.append("</table>");
    }
}
