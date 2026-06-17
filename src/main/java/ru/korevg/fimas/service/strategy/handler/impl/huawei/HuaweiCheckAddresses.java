package ru.korevg.fimas.service.strategy.handler.impl.huawei;


import com.jcraft.jsch.JSchException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.korevg.fimas.config.AppConstants;
import ru.korevg.fimas.dto.firewall.FirewallResponse;
import ru.korevg.fimas.entity.Address;
import ru.korevg.fimas.entity.Command;
import ru.korevg.fimas.exception.EntityNotFoundException;
import ru.korevg.fimas.service.AddressService;
import ru.korevg.fimas.service.FirewallService;
import ru.korevg.fimas.service.strategy.handler.LocalCommandHandler;
import ru.korevg.fimas.service.strategy.handler.impl.HtmlReportAddressService;
import ru.korevg.fimas.util.FileTool;
import ru.korevg.fimas.util.SshExecutor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class HuaweiCheckAddresses implements LocalCommandHandler {

    private final FirewallService firewallService;
    private final AddressService addressService;
    private final SshExecutor sshExecutor;
    private final HtmlReportAddressService htmlReportAddressService;
    private final FileTool fileTool;

    private static final String DISABLE_PAGE_MODE = "screen-length 0 temporary";
    private static final String GET_ADDRESS_COMMAND = "display ip address-set type object";
    private static final Pattern NAME_GROUP_PATTERN = Pattern.compile("Address-set:\\s*(.+)");


    @Override
    public String handle(Command command, Long firewallId, String username, String password) {
        FirewallResponse firewall = firewallService.findById(firewallId)
                .orElseThrow(() -> new EntityNotFoundException("Firewall с ID: " + firewallId + " не найден"));

        log.info("Проверка адрессных групп на Firewall: {}", firewall.name());

        String rawAddrGroups = getAddressFromRemoteFW(firewall.mgmtIpAddress(), username, password);
//        String rawAddrGroups = fileTool.readFileToString("adresses.txt");

        log.info(rawAddrGroups);

        Map<String, List<String>> remoteAddrGroups = parseAddrGroups(rawAddrGroups);
        List<Address> localAddresses = addressService.findAllByFirewallId(firewallId);

        return htmlReportAddressService.buildComparisonHtml(localAddresses, remoteAddrGroups, firewall.name());
    }

    /**
     * Получение адресов с удалённого FW по SSH
     */
    private String getAddressFromRemoteFW(String ipAddress, String username, String password) {
        try {
            sshExecutor.createSshSession(ipAddress, AppConstants.SSH, username, password);
            List<String> commands = List.of(DISABLE_PAGE_MODE, GET_ADDRESS_COMMAND);
            return sshExecutor.executeSshCommands(commands);
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

    private Map<String, List<String>> parseAddrGroups(String rawAddrGroups) {
        Map<String, List<String>> result = new HashMap<>();
        String[] blocks = rawAddrGroups.split("(?=Address-set:\\s)");

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

        Pattern pattern = Pattern.compile(
                "address\\s+\\d+\\s+(\\S+)\\s+mask\\s+(\\d+)"
        );

        return Arrays.stream(block.split("\\n"))
                .map(String::trim)
                .map(pattern::matcher)
                .filter(Matcher::find)
                .map(m -> {
                    String ip =  m.group(1).trim();
                    String mask = m.group(2).trim();
                    if (!mask.equals("32")) {
                        ip += "/" + mask;
                    }
                    return ip;
                })
                .toList();
    }

    @Override
    public String getCommandKey() {
        return "check addresses";
    }

    @Override
    public String getVendorKey() {
        return AppConstants.HUAWEI;
    }
}
