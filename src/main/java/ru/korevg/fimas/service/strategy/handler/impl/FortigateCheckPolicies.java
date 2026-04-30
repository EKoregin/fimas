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
import ru.korevg.fimas.entity.PolicyAction;
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
            List<String> dstAddr
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
                getFirstOrEmpty(remote, "action"),
                remote.getOrDefault("srcaddr", List.of()),
                remote.getOrDefault("dstaddr", List.of())
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
                .append(label).append(" — ").append(localValue).append(" | ").append(remoteValue)
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


//    private void appendLocalPoliciesTable(StringBuilder sb,
//                                          List<Policy> localPolicies,
//                                          Map<String, Map<String, List<String>>> remotePolicies,
//                                          AtomicInteger counter) {
//
//        sb.append("<h3>Политики из базы</h3>");
//        sb.append("<table style='border-collapse: collapse; width: 100%;'>");
//        sb.append("<tr style='background-color: #f0f0f0;'><th>№</th><th>Правило</th><th>Статус на FW</th></tr>");
//
//        for (Policy policy : localPolicies) {
//            String status = "Отсутствует на FW";
//            String style = "color: red;";
//
//
//            String remoteSrcZone = "";
//            String remoteDstZone = "";
//            String remoteAction = "";
//            List<String> remoteSrcAddr = new ArrayList<>();
//            List<String> remoteDstAddr = new ArrayList<>();
//            String localSrcZone = policy.getSrcZone().getName();
//            String localDstZone = policy.getDstZone().getName();
//            String localAction = policy.getAction().equals(PolicyAction.PERMIT) ? "accept" : "deny";
//            List<String> localSrcAddr = policy.getSrcAddresses().stream().map(Address::getName).toList();
//            List<String> localDstAddr = policy.getDstAddresses().stream().map(Address::getName).toList();
//
//            if (remotePolicies.containsKey(policy.getName())) {
//                Map<String, List<String>> remote = remotePolicies.get(policy.getName());
//                status = remote.getOrDefault("status", List.of("Enabled")).getFirst();
//                style = "color: green;";
//                remoteSrcZone = remote.get("srcintf").getFirst();
//                remoteDstZone = remote.get("dstintf").getFirst();
//                remoteAction = remote.getOrDefault("action", List.of("deny")).getFirst();
//                remoteSrcAddr = remote.getOrDefault("srcaddr", List.of());
//                remoteDstAddr = remote.getOrDefault("dstaddr", List.of());
//            }
//
//            sb.append("<tr>")
//                    .append("<td>").append(counter.getAndIncrement()).append("</td>")
//                    .append("<td>").append(policy.getName()).append("</td>")
//                    .append("<td style='").append(style).append("'>").append(status).append("</td>")
//                    .append("</tr>");
//            //Проверка зон
//            sb.append("<tr>")
//                    .append("<td></td>")
//                    .append("<td style='text-align: right;'>Source Zone - ").append("Local: ").append(localSrcZone).append(" | Remote: ").append(remoteSrcZone).append("</td>")
//                    .append("<td>").append(localSrcZone.equals(remoteSrcZone) ? "OK" : "FALSE").append("</td>")
//                    .append("</tr>");
//            sb.append("<tr>")
//                    .append("<td></td>")
//                    .append("<td style='text-align: right;'>Destination Zone - ").append("Local: ").append(localDstZone).append(" | Remote: ").append(remoteDstZone).append("</td>")
//                    .append("<td>").append(localDstZone.equals(remoteDstZone) ? "OK" : "FALSE").append("</td>")
//                    .append("</tr>");
//            //Проверка Action
//            sb.append("<tr>")
//                    .append("<td></td>")
//                    .append("<td style='text-align: right;'>Action - ").append("Local: ").append(localAction).append(" | Remote: ").append(remoteAction).append("</td>")
//                    .append("<td>").append(localAction.equals(remoteAction) ? "OK" : "FALSE").append("</td>")
//                    .append("</tr>");
//            //Проверка адресов. Вывести только отличающиеся
//            List<String> finalRemoteSrcAddr = remoteSrcAddr;
//            sb.append("<tr>")
//                    .append("<td></td>")
//                    .append("<td style='text-align: right;'>Source Addr - ").append("Local: <br>Remote: ").append("</td>")
//                    .append("<td>").append(localSrcAddr.stream().filter(address -> !finalRemoteSrcAddr.contains(address)).toList()).append("<br>")
//                    .append(remoteSrcAddr.stream().filter(address -> !localSrcAddr.contains(address)).toList()).append("<br>")
//                    .append("</td>")
//                    .append("</tr>");
//            List<String> finalRemoteDstAddr = remoteDstAddr;
//            sb.append("<tr>")
//                    .append("<td></td>")
//                    .append("<td style='text-align: right;'>Destination Addr - ").append("Local: <br>Remote: ").append("</td>")
//                    .append("<td>").append(localDstAddr.stream().filter(address -> !finalRemoteDstAddr.contains(address)).toList()).append("<br>")
//                    .append(remoteDstAddr.stream().filter(address -> !localDstAddr.contains(address)).toList()).append("<br>")
//                    .append("</td>")
//                    .append("</tr>");
//        }
//
//        sb.append("</table>");
//    }

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


//package ru.korevg.fimas.service.strategy.handler.impl;
//
//import com.jcraft.jsch.JSchException;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//import ru.korevg.fimas.config.AppConstants;
//import ru.korevg.fimas.dto.firewall.FirewallResponse;
//import ru.korevg.fimas.dto.policy.PolicyResponse;
//import ru.korevg.fimas.entity.Command;
//import ru.korevg.fimas.exception.EntityNotFoundException;
//import ru.korevg.fimas.service.FirewallService;
//import ru.korevg.fimas.service.PolicyService;
//import ru.korevg.fimas.service.strategy.handler.LocalCommandHandler;
//import ru.korevg.fimas.util.SshExecutor;
//
//import java.util.*;
//import java.util.concurrent.atomic.AtomicInteger;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class FortigateCheckPolicies implements LocalCommandHandler {
//
//    private final PolicyService policyService;
//    private final FirewallService firewallService;
//    private final SshExecutor sshExecutor;
//
//    private static final String GET_POLICIES = "show firewall policy";
//
//    @Override
//    public String handle(Command command, Long firewallId, String username, String password) {
//        FirewallResponse firewall = firewallService.findById(firewallId)
//                .orElseThrow(() -> new EntityNotFoundException("Firewall c ID: " + firewallId + "не найден"));
//        List<PolicyResponse> localPolicies = policyService.findByFirewallId(firewallId);
//        log.info("Проверка и сравнение политик на удаленном FW: {}", firewall.name());
//        String rawPolicies = getPoliciesRemoteFW(username, password, firewall.mgmtIpAddress());
//        Map<String, Map<String, List<String>>> remotePolicies = policyParser(rawPolicies);
//
//        return policyVerificatorHtml(localPolicies, remotePolicies);
//    }
//
//    private String policyVerificatorHtml(List<PolicyResponse> localPolicies, Map<String, Map<String, List<String>>> remotePolicies) {
//        StringBuilder sb = new StringBuilder();
//        AtomicInteger remoteExtraEnabled = new AtomicInteger();
//        AtomicInteger remoteExtraDisabled = new AtomicInteger();
//
//        sb.append("<div>");
//        sb.append("<style>");
//        sb.append("  table { border-collapse: collapse; width: 100%; }");
//        sb.append("  th, td { border: 1px solid #ccc; padding: 10px; }");
//        sb.append("</style>");
//        sb.append("<table>");
//        sb.append("<tr><th>№</th><th>Правило</th><th>Статус</th></tr>");
//
//        AtomicInteger lineNumber = new AtomicInteger(1);
//        localPolicies.forEach(policy -> {
//            if (remotePolicies.containsKey(policy.name())) {
//                Map<String, List<String>> remotePolicy = remotePolicies.get(policy.name());
//                String status = "Enabled";
//                if (remotePolicy.containsKey("status")) {
//                    status = remotePolicy.get("status").toString();
//                }
//                sb.append("<tr>")
//                        .append("<td>").append(lineNumber.getAndIncrement()).append("</td>")
//                        .append("<td>").append(policy.name()).append("</td>")
//                        .append("<td>").append(status).append("</td>");
//            } else {
//                sb.append("<tr>")
//                        .append("<td>").append(lineNumber.getAndIncrement()).append("</td>")
//                        .append("<td>").append(policy.name()).append("</td>")
//                        .append("<td sty>").append("Политики нет на удаленном Firewall!").append("</td>");
//            }
//            sb.append("</tr>");
//        });
//        sb.append("</table>");
//
//        sb.append("<h3>Политики которых нет в БАЗЕ, но есть на FW</h3>");
//        sb.append("<table>");
//        sb.append("<tr><th>№</th><th>Правило</th><th>Статус</th></tr>");
//        Set<String> localPolicyNameSet = localPolicies.stream()
//                .map(PolicyResponse::name)
//                .collect(Collectors.toSet());
//        remotePolicies.forEach((key, value) -> {
//            if (!localPolicyNameSet.contains(key)) {
//                String status = "Enabled";
//                if (remotePolicies.get(key).containsKey("status")) {
//                    status = remotePolicies.get(key).get("status").toString();
//                }
//                if (status.equals("Enabled")) {
//                    remoteExtraEnabled.getAndIncrement();
//                } else {
//                    remoteExtraDisabled.getAndIncrement();
//                }
//                sb.append("<tr>")
//                        .append("<td>").append(lineNumber.getAndIncrement()).append("</td>")
//                        .append("<td>").append(key).append("</td>")
//                        .append("<td>").append(status).append("</td>")
//                        .append("</tr>");
//            }
//        });
//        sb.append("</table>");
//        sb.append("<p>Всего дополнительных политик: ").append(remoteExtraEnabled.get() + remoteExtraDisabled.get()).append("</p>");
//        sb.append("<p>Из них включенных: ").append(remoteExtraEnabled.get()).append("</p>");
//        sb.append("<p>Из них выключенных: ").append(remoteExtraDisabled).append("</p>");
//        sb.append("</div>");
//
//        return sb.toString();
//    }
//
//    private Map<String, Map<String, List<String>>> policyParser(String rawPolicies) {
//        Map<String, Map<String, List<String>>> result = new HashMap<>();
//        String[] blocks = rawPolicies.split("(?=edit)");
//
//        for (String block : blocks) {
//            if (block.trim().isEmpty()) {
//                continue;
//            }
//
//            String policyName = extractName(block);
//            if (policyName == null) {
//                continue;
//            }
//
//            Map<String, List<String>> parameters = new HashMap<>();
//            extractPolicyParameters(block, parameters);
//            result.put(policyName, parameters);
//        }
//
//        return result;
//    }
//
//    private static String extractName(String block) {
//        Pattern pattern = Pattern.compile("set name \"([^\"]+)\"");
//        Matcher matcher = pattern.matcher(block);
//        return matcher.find() ? matcher.group(1) : null;
//    }
//
//    private static void extractPolicyParameters(String block, Map<String, List<String>> parameters) {
//        String[] lines = block.split("\\n");
//        for (String line : lines) {
//            line = line.trim();
//            if (line.startsWith("set ")) {
//                String[] parts = line.split(" ", 3); // Разбиваем по пробелам, чтобы получить ключ и значения
//                if (parts.length < 3) {
//                    continue; // Если нет достаточно частей, пропускаем
//                }
//
//                String key = parts[1];
//                String[] values = parts[2].replace("\"", "").split("\\s+"); // Убираем кавычки и разбиваем по пробелам
//
//                parameters.putIfAbsent(key, new ArrayList<>());
//                parameters.get(key).addAll(Arrays.asList(values));
//            }
//        }
//    }
//
//    private String getPoliciesRemoteFW(String username, String password, String fwMgmtIp) {
//        log.info("Получение политик с удаленного Firewall");
//
//        try {
//            sshExecutor.createSshSession(fwMgmtIp, AppConstants.SSH, username, password);
//            return sshExecutor.executeSshCommand(GET_POLICIES);
//        } catch (JSchException e) {
//            String msg = String.format("Ошибка SSH-подключения к %s:%d → %s", fwMgmtIp, AppConstants.SSH, e.getMessage());
//            log.error(msg, e);
//            throw new RuntimeException(msg, e);
//        } catch (Exception e) {
//            String msg = String.format("Ошибка выполнения действия '%s' на %s: %s",
//                    GET_POLICIES, fwMgmtIp, e.getMessage());
//            log.error(msg, e);
//            throw new RuntimeException(msg, e);
//        } finally {
//            sshExecutor.disconnect();
//        }
//
//    }
//
//    @Override
//    public String getCommandKey() {
//        return "check policies";
//    }
//
//    @Override
//    public String getVendorKey() {
//        return AppConstants.FORTIGATE;
//    }
//}
