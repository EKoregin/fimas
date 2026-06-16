package ru.korevg.fimas.service.strategy.handler.impl.huawei;

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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class HuaweiCheckPolicies implements LocalCommandHandler {

    private final PolicyRepository policyRepository;
    private final FirewallService firewallService;
    private final SshExecutor sshExecutor;
    private final HtmlReportPoliciesService htmlReportPoliciesService;

    private static final String GET_POLICIES_COMMAND = "display current-configuration | begin security-policy";
    private static final String DISABLE_PAGE_MODE = "screen-length 0 temporary";

    private static final Pattern NAME_PATTERN = Pattern.compile("rule\\s+name\\s+([\\S]+)", Pattern.CASE_INSENSITIVE);

    @Override
    public String handle(Command command, Long firewallId, String username, String password) {
        FirewallResponse firewall = firewallService.findById(firewallId)
                .orElseThrow(() -> new EntityNotFoundException("Firewall с ID: " + firewallId + " не найден"));

        List<Policy> localPolicies = policyRepository.findByFirewallId(firewallId);

        log.info("Проверка политик на Firewall: {}", firewall.name());

        String rawPolicies = getPoliciesFromRemote(firewall.mgmtIpAddress(), username, password);
//        String rawPolicies = readFileToString("policies.txt");
        String cutPolicies = cutExcludedOutput(rawPolicies);
        Map<String, Map<String, List<String>>> remotePolicies = parsePolicies(cutPolicies);

        return htmlReportPoliciesService.buildComparisonHtml(localPolicies, remotePolicies, firewall.name(), getVendorKey());
    }

    public String readFileToString(String filePath) {
        System.out.println("Текущая рабочая директория: " +
                Path.of("").toAbsolutePath().normalize());
        String result = "";
        try {
            Path path = Path.of(filePath);
            result = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
        return result;
    }

    /**
     * Получение сырых политик с удалённого FW по SSH (требуется парсинг)
     */
    private String getPoliciesFromRemote(String ipAddress, String username, String password) {
        try {
            sshExecutor.createSshSession(ipAddress, AppConstants.SSH, username, password);
            List<String> commands = List.of(DISABLE_PAGE_MODE, GET_POLICIES_COMMAND);
            return sshExecutor.executeSshCommands(commands);
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
     * Парсинг сырых политик
     */
    private Map<String, Map<String, List<String>>> parsePolicies(String rawPolicies) {
        Map<String, Map<String, List<String>>> result = new LinkedHashMap<>();

        String[] blocks = rawPolicies.split("(?=rule\\s)");

        for (String block : blocks) {
            if (block.trim().isEmpty()) continue;

            String policyName = extractPolicyName(block);
            if (policyName == null) continue;

            Map<String, List<String>> parameters = extractPolicyParameters(block);
            result.put(policyName, parameters);
        }

        return result;
    }

    private String cutExcludedOutput(String rawPolicies) {
        int startIndex = rawPolicies.indexOf("rule");
        int endIndex = rawPolicies.indexOf("group name");
        return rawPolicies.substring(startIndex, endIndex);
    }

    private String extractPolicyName(String block) {
        Matcher matcher = NAME_PATTERN.matcher(block);
        return matcher.find() ? matcher.group(1) : null;
    }

    private Map<String, List<String>> extractPolicyParameters(String block) {
        Map<String, List<String>> parameters = new LinkedHashMap<>();

        for (String line : block.split("\\r?\\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("rule name ")) {
                continue;
            }

            int firstSpace = line.indexOf(' ');
            if (firstSpace <= 0) continue;

            String key = line.substring(0, firstSpace);
            String value = line.substring(firstSpace + 1).trim();

            if (value.startsWith("address-set ")) {
                value = value.substring("address-set ".length()).trim();
            }
            if (value.startsWith("domain-set ")) {
                value = value.substring("domain-set ".length()).trim();
            }

            parameters.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }

        parameters.computeIfAbsent("source-zone", k -> new ArrayList<>()).add("ANY");
        parameters.computeIfAbsent("destination-zone", k -> new ArrayList<>()).add("ANY");

        return parameters;
    }

    @Override
    public String getCommandKey() {
        return "check policies";
    }

    @Override
    public String getVendorKey() {
        return AppConstants.HUAWEI;
    }
}