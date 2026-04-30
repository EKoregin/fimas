package ru.korevg.fimas.service.strategy.handler.impl;

import com.jcraft.jsch.JSchException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.korevg.fimas.config.AppConstants;
import ru.korevg.fimas.dto.firewall.FirewallResponse;
import ru.korevg.fimas.entity.*;
import ru.korevg.fimas.exception.EntityNotFoundException;
import ru.korevg.fimas.repository.PolicyRepository;
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
public class FortigateCheckPolicies implements LocalCommandHandler {

    private final PolicyRepository policyRepository;
    private final FirewallService firewallService;
    private final SshExecutor sshExecutor;

    private static final String GET_POLICIES_COMMAND = "show firewall policy";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private static final Pattern NAME_PATTERN = Pattern.compile("set name \"([^\"]+)\"");

    @Override
    public String handle(Command command, Long firewallId, String username, String password) {
        FirewallResponse firewall = firewallService.findById(firewallId)
                .orElseThrow(() -> new EntityNotFoundException("Firewall с ID: " + firewallId + " не найден"));

        List<Policy> localPolicies = policyRepository.findByFirewallId(firewallId);

        log.info("Проверка политик на Firewall: {}", firewall.name());

        String rawPolicies = getPoliciesFromRemote(firewall.mgmtIpAddress(), username, password);
        Map<String, Map<String, List<String>>> remotePolicies = parsePolicies(rawPolicies);

        return buildComparisonHtml(localPolicies, remotePolicies, firewall.name());
    }

    /**
     * Получение политик с удалённого Fortigate по SSH
     */
    private String getPoliciesFromRemote(String ipAddress, String username, String password) {
        try {
            sshExecutor.createSshSession(ipAddress, AppConstants.SSH, username, password);
            return sshExecutor.executeSshCommand(GET_POLICIES_COMMAND);
        } catch (JSchException e) {
            log.error("Ошибка SSH-подключения к {}: {}", ipAddress, e.getMessage(), e);
            throw new RuntimeException("Не удалось подключиться к Firewall " + ipAddress, e);
        } catch (Exception e) {
            log.error("Ошибка выполнения команды '{}' на {}: {}", GET_POLICIES_COMMAND, ipAddress, e.getMessage(), e);
            throw new RuntimeException("Ошибка получения политик с Firewall", e);
        } finally {
            sshExecutor.disconnect();
        }
    }

    /**
     * Парсинг сырых политик Fortigate
     */
    private Map<String, Map<String, List<String>>> parsePolicies(String rawPolicies) {
        Map<String, Map<String, List<String>>> result = new LinkedHashMap<>();

        String[] blocks = rawPolicies.split("(?=edit\\s)");

        for (String block : blocks) {
            if (block.trim().isEmpty()) continue;

            String policyName = extractPolicyName(block);
            if (policyName == null) continue;

            Map<String, List<String>> parameters = extractPolicyParameters(block);
            result.put(policyName, parameters);
        }

        return result;
    }

    private String extractPolicyName(String block) {
        Matcher matcher = NAME_PATTERN.matcher(block);
        return matcher.find() ? matcher.group(1) : null;
    }

    private Map<String, List<String>> extractPolicyParameters(String block) {
        Map<String, List<String>> parameters = new LinkedHashMap<>();
        String[] lines = block.split("\\n");

        for (String line : lines) {
            line = line.trim();
            if (!line.startsWith("set ")) continue;

            String[] parts = line.split("\\s+", 3);
            if (parts.length < 3) continue;

            String key = parts[1];
            String valuePart = parts[2].replace("\"", "").trim();

            parameters.computeIfAbsent(key, k -> new ArrayList<>())
                    .addAll(Arrays.asList(valuePart.split("\\s+")));
        }
        return parameters;
    }

    /**
     * Основной метод построения HTML-отчёта
     */
    private String buildComparisonHtml(List<Policy> localPolicies,
                                       Map<String, Map<String, List<String>>> remotePolicies,
                                       String firewallName) {

        StringBuilder sb = new StringBuilder();

        AtomicInteger counter = new AtomicInteger(1);
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
        appendLocalPoliciesTable(sb, localPolicies, remotePolicies, counter);

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
                                          AtomicInteger counter) {

        sb.append("<h3>Политики из базы</h3>");
        sb.append("<table style='border-collapse: collapse; width: 100%;'>");
        sb.append("<tr style='background-color: #f0f0f0;'>")
                .append("<th>№</th>")
                .append("<th>Правило</th>")
                .append("<th>Статус на FW</th>")
                .append("</tr>");

        for (Policy policy : localPolicies) {
            RemotePolicyData remote = getRemotePolicyData(remotePolicies, policy.getName());

            String status = remote != null ? remote.status() : "Отсутствует на FW";
            String statusStyle = remote != null ? "color: green;" : "color: red;";

            // Основная строка политики
            sb.append("<tr>")
                    .append("<td>").append(counter.getAndIncrement()).append("</td>")
                    .append("<td>").append(policy.getName()).append("</td>")
                    .append("<td style='").append(statusStyle).append("'>").append(status).append("</td>")
                    .append("</tr>");

            // Детальное сравнение (только если политика есть на удалённом FW)
            if (remote != null) {
                appendPolicyComparisonRows(sb, policy, remote, counter);
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
    ) {}

    private RemotePolicyData getRemotePolicyData(Map<String, Map<String, List<String>>> remotePolicies, String policyName) {
        Map<String, List<String>> remote = remotePolicies.get(policyName);
        if (remote == null) {
            return null;
        }

        return new RemotePolicyData(
                remote.getOrDefault("status", List.of("Enabled")).getFirst(),
                getFirstOrEmpty(remote, "srcintf"),
                getFirstOrEmpty(remote, "dstintf"),
//                getFirstOrEmpty(remote, "action").isEmpty() ? "deny" : "accept",
                remote.getOrDefault("action", List.of("deny")).getFirst(),
                remote.getOrDefault("srcaddr", List.of()),
                remote.getOrDefault("dstaddr", List.of()),
                remote.getOrDefault("service", List.of())
        );
    }

    private String getFirstOrEmpty(Map<String, List<String>> map, String key) {
        List<String> list = map.get(key);
        return (list != null && !list.isEmpty()) ? list.getFirst() : "";
    }
    // Вывод детального сравнения (зоны, action, адреса)
    private void appendPolicyComparisonRows(StringBuilder sb,
                                            Policy policy,
                                            RemotePolicyData remote,
                                            AtomicInteger counter) {

        // Source Zone
        appendComparisonRow(sb, counter, "Source Zone",
                "Local: " + policy.getSrcZone().getName(),
                "Remote: " + remote.srcZone(),
                policy.getSrcZone().getName().equals(remote.srcZone()));

        // Destination Zone
        appendComparisonRow(sb, counter, "Destination Zone",
                "Local: " + policy.getDstZone().getName(),
                "Remote: " + remote.dstZone(),
                policy.getDstZone().getName().equals(remote.dstZone()));

        // Action
        String localAction = policy.getAction().equals(PolicyAction.PERMIT) ? "accept" : "deny";
        appendComparisonRow(sb, counter, "Action",
                "Local: " + localAction,
                "Remote: " + remote.action(),
                localAction.equals(remote.action()));

        // Source Addresses (только различия)
        appendAddressComparisonRow(sb, counter, "Source Addr",
                policy.getSrcAddresses().stream().map(Address::getName).toList(),
                remote.srcAddr());

        // Destination Addresses (только различия)
        appendAddressComparisonRow(sb, counter, "Destination Addr",
                policy.getDstAddresses().stream().map(Address::getName).toList(),
                remote.dstAddr());

        // Services (только различия)
        appendAddressComparisonRow(sb, counter, "Services",
                policy.getServices().stream().map(Service::getName).toList(),
                remote.services);
    }

    private void appendComparisonRow(StringBuilder sb,
                                     AtomicInteger counter,
                                     String label,
                                     String localValue,
                                     String remoteValue,
                                     boolean isMatch) {

        sb.append("<tr>")
                .append("<td></td>")
                .append("<td style='text-align: right; font-weight: bold;'>")
                .append(label)
//                .append(label).append(" — ").append(localValue).append(" | ").append(remoteValue)
                .append("</td>")
                .append("<td style='color: ").append(isMatch ? "green" : "red").append(";'>")
                .append(isMatch ? "OK" : "FALSE")
                .append("</td>")
                .append("</tr>");
    }

    private void appendAddressComparisonRow(StringBuilder sb,
                                            AtomicInteger counter,
                                            String label,
                                            List<String> localAddrs,
                                            List<String> remoteAddrs) {

        List<String> onlyInLocal = localAddrs.stream()
                .filter(addr -> !remoteAddrs.contains(addr))
                .toList();

        List<String> onlyInRemote = remoteAddrs.stream()
                .filter(addr -> !localAddrs.contains(addr))
                .toList();

        if (onlyInLocal.isEmpty() && onlyInRemote.isEmpty()) {
            return; // если адреса идентичны — не выводим строку
        }

        sb.append("<tr>")
                .append("<td></td>")
                .append("<td style='text-align: right; font-weight: bold;'>")
                .append(label).append(" — Local: <br>Remote: ")
                .append("</td>")
                .append("<td>")
                .append("Только в Local: ").append(onlyInLocal).append("<br>")
                .append("Только в Remote: ").append(onlyInRemote)
                .append("</td>")
                .append("</tr>");
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

        sb.append("<h3>Политики, которых нет в базе, но есть на Firewall</h3>");
        sb.append("<table style='border-collapse: collapse; width: 100%;'>");
        sb.append("<tr style='background-color: #f0f0f0;'><th>№</th><th>Правило</th><th>Статус</th></tr>");

        remotePolicies.forEach((name, params) -> {
            if (!localNames.contains(name)) {
                String status = params.getOrDefault("status", List.of("Enabled")).get(0);
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

    @Override
    public String getCommandKey() {
        return "check policies";
    }

    @Override
    public String getVendorKey() {
        return AppConstants.FORTIGATE;
    }
}