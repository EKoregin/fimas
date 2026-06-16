package ru.korevg.fimas.service.strategy.handler.impl.fortigate;

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
import ru.korevg.fimas.service.strategy.handler.impl.HtmlReportPoliciesService;
import ru.korevg.fimas.util.SshExecutor;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class FortigateCheckPolicies implements LocalCommandHandler {

    private final PolicyRepository policyRepository;
    private final FirewallService firewallService;
    private final SshExecutor sshExecutor;
    private final HtmlReportPoliciesService htmlReportPoliciesService;

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

        return htmlReportPoliciesService.buildComparisonHtml(localPolicies, remotePolicies, firewall.name(), getVendorKey());
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

    @Override
    public String getCommandKey() {
        return "check policies";
    }

    @Override
    public String getVendorKey() {
        return AppConstants.FORTIGATE;
    }
}