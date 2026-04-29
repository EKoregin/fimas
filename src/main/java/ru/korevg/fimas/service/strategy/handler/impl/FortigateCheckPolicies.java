package ru.korevg.fimas.service.strategy.handler.impl;

import com.jcraft.jsch.JSchException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.korevg.fimas.config.AppConstants;
import ru.korevg.fimas.dto.firewall.FirewallResponse;
import ru.korevg.fimas.dto.policy.PolicyResponse;
import ru.korevg.fimas.entity.Command;
import ru.korevg.fimas.exception.EntityNotFoundException;
import ru.korevg.fimas.service.FirewallService;
import ru.korevg.fimas.service.PolicyService;
import ru.korevg.fimas.service.strategy.handler.LocalCommandHandler;
import ru.korevg.fimas.util.SshExecutor;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class FortigateCheckPolicies implements LocalCommandHandler {

    private final PolicyService policyService;
    private final FirewallService firewallService;
    private final SshExecutor sshExecutor;

    private static final String GET_POLICIES = "show firewall policy";

    @Override
    public String handle(Command command, Long firewallId, String username, String password) {
        FirewallResponse firewall = firewallService.findById(firewallId)
                .orElseThrow(() -> new EntityNotFoundException("Firewall c ID: " + firewallId + "не найден"));
        List<PolicyResponse> localPolicies = policyService.findByFirewallId(firewallId);
        log.info("Проверка и сравнение политик на удаленном FW: {}", firewall.name());
        String rawPolicies = getPoliciesRemoteFW(firewallId, username, password, firewall);
        Map<String, Map<String, List<String>>> remotePolicies = policyParser(rawPolicies);

        return policyVerificator(localPolicies, remotePolicies);
    }

    private String policyVerificator(List<PolicyResponse> localPolicies, Map<String, Map<String, List<String>>> remotePolicies) {
        StringBuilder sb = new StringBuilder();
        AtomicInteger remoteExtraEnabled = new AtomicInteger();
        AtomicInteger remoteExtraDisabled = new AtomicInteger();
        sb.append("Проверка соответствия локальных политик").append("\n");
        localPolicies.forEach(policy -> {
                    if (remotePolicies.containsKey(policy.name())) {
                        Map<String, List<String>> remotePolicy = remotePolicies.get(policy.name());
                        String status = "Enabled";
                        if (remotePolicy.containsKey("status")) {
                            status = remotePolicy.get("status").toString();
                        }
                        sb.append(policy.name())
                                .append(" --- Remote status: ")
                                .append(status)
                                .append("\n");
                    } else {
                        sb.append(policy.name())
                                .append("--- !!!Политики нет!!!")
                                .append("\n");
                    }
                });
        sb.append("\n\n\nПолитики которых нет в БАЗЕ, но есть на FW\n");
        Set<String> localPolicyNameSet = localPolicies.stream()
                .map(PolicyResponse::name)
                .collect(Collectors.toSet());
        remotePolicies.forEach((key, value) -> {
            if (!localPolicyNameSet.contains(key)) {
                String status = "Enabled";
                if (remotePolicies.get(key).containsKey("status")) {
                    status = remotePolicies.get(key).get("status").toString();
                }
                if (status.equals("Enabled")) {
                    remoteExtraEnabled.getAndIncrement();
                } else {
                    remoteExtraDisabled.getAndIncrement();
                }
                sb.append(key).append(" --- Remote status: ")
                        .append(status)
                        .append("\n");
            }
        });
        sb.append("Всего дополнительных политик: ").append(remoteExtraEnabled.get() + remoteExtraDisabled.get()).append("\n");
        sb.append("Из них включенных: ").append(remoteExtraEnabled.get()).append("\n");
        sb.append("Из них выключенных: ").append(remoteExtraDisabled).append("\n");
        return formatText(sb.toString());
    }

    private String formatText(String input) {
        String[] lines = input.split("\n");
        int maxLength = 0;

        // Находим максимальную длину первых частей строк
        for (String line : lines) {
            maxLength = Math.max(maxLength, line.split(" --- ")[0].length());
        }

        // Форматируем и собираем результат
        StringBuilder output = new StringBuilder();
        for (String line : lines) {
            String[] parts = line.split(" --- ");
            output.append(parts[0])
                    .append(" ".repeat(maxLength - parts[0].length() + 3))
                    .append(parts.length > 1 ? parts[1] : "")
                    .append("\n");
        }

        return output.toString();
    }

    private Map<String, Map<String, List<String>>> policyParser(String rawPolicies) {
        Map<String, Map<String, List<String>>> result = new HashMap<>();
        String[] blocks = rawPolicies.split("(?=edit)");

        for (String block : blocks) {
            if (block.trim().isEmpty()) {
                continue;
            }

            String policyName = extractName(block);
            if (policyName == null) {
                continue;
            }

            Map<String, List<String>> parameters = new HashMap<>();
            extractPolicyParameters(block, parameters);
            result.put(policyName, parameters);
        }

        return result;
    }

    private static String extractName(String block) {
        Pattern pattern = Pattern.compile("set name \"([^\"]+)\"");
        Matcher matcher = pattern.matcher(block);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static void extractPolicyParameters(String block, Map<String, List<String>> parameters) {
        String[] lines = block.split("\\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("set ")) {
                String[] parts = line.split(" ", 3); // Разбиваем по пробелам, чтобы получить ключ и значения
                if (parts.length < 3) {
                    continue; // Если нет достаточно частей, пропускаем
                }

                String key = parts[1];
                String[] values = parts[2].replace("\"", "").split("\\s+"); // Убираем кавычки и разбиваем по пробелам

                parameters.putIfAbsent(key, new ArrayList<>());
                parameters.get(key).addAll(Arrays.asList(values));
            }
        }
    }

    private String getPoliciesRemoteFW(Long firewallId, String username, String password, FirewallResponse firewall) {
        log.info("Получение политик с удаленного Firewall");

        try {
            sshExecutor.createSshSession(firewall.mgmtIpAddress(), AppConstants.SSH, username, password);
            return sshExecutor.executeSshCommand(GET_POLICIES);
        } catch (JSchException e) {
            String msg = String.format("Ошибка SSH-подключения к %s:%d → %s", firewall.mgmtIpAddress(), AppConstants.SSH, e.getMessage());
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        } catch (Exception e) {
            String msg = String.format("Ошибка выполнения действия '%s' на %s: %s",
                    GET_POLICIES, firewall.mgmtIpAddress(), e.getMessage());
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        } finally {
            sshExecutor.disconnect();
        }

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
