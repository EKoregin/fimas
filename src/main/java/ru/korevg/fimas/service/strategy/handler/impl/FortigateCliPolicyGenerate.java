package ru.korevg.fimas.service.strategy.handler.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.korevg.fimas.config.AppConstants;
import ru.korevg.fimas.dto.address.AddressShortResponse;
import ru.korevg.fimas.dto.policy.PolicyResponse;
import ru.korevg.fimas.dto.service.ServiceShortResponse;
import ru.korevg.fimas.entity.Command;
import ru.korevg.fimas.entity.PolicyAction;
import ru.korevg.fimas.entity.Port;
import ru.korevg.fimas.entity.Protocol;
import ru.korevg.fimas.entity.Service;
import ru.korevg.fimas.repository.ServiceRepository;
import ru.korevg.fimas.service.PolicyService;
import ru.korevg.fimas.service.ServiceService;
import ru.korevg.fimas.service.strategy.handler.LocalCommandHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Component
@RequiredArgsConstructor
public class FortigateCliPolicyGenerate implements LocalCommandHandler {

    private final PolicyService policyService;


    @Override
    public String handle(Command command, Long firewallId, String username, String password) {
        log.info("Создание конфигурации CLI для политик Fortigate: {}", command.getName());
        var policies = policyService.findByFirewallId(firewallId);

        String configBody = policies.stream()
                .map(this::policyToEditBlock)
                .collect(Collectors.joining("\n"));

        log.info("Создание конфигурации завершено");

        return """
                <div>
                <h1>config firewall policy</h1>
                <h4>-=Все политики по умолчанию выключены=-</h4>
                <p>
                config firewall policy<br>
                %s
                end
                </p>
                <p>Всего %s политик</p>
                </div>
                """.formatted(configBody, policies.size());
    }

    private String policyToEditBlock(PolicyResponse policy) {
        String name = policy.name();
        String srcZone = policy.srcZone().name();
        String dstZone = policy.dstZone().name();
        var srcAddr = policy.srcAddresses();
        var dstAddr = policy.dstAddresses();
        String action = policy.action().name();
        var service = policy.services();


        StringBuilder sb = new StringBuilder();
        sb.append("edit 0<br>");
        sb.append("    set status disable<br>");
        sb.append("    set name \"").append(name).append("\"<br>");
        sb.append("    set srcintf \"").append(srcZone).append("\"<br>");
        sb.append("    set dstintf \"").append(dstZone).append("\"<br>");
        sb.append("    set srcaddr \"")
                .append(srcAddr.isEmpty() ? "all" : srcAddr.stream()
                        .map(AddressShortResponse::name)
                        .collect(Collectors.joining("\" \"")))
                .append("\"<br>");
        sb.append("    set dstaddr \"")
                .append(dstAddr.isEmpty() ? "all" : dstAddr.stream()
                        .map(AddressShortResponse::name)
                        .collect(Collectors.joining("\" \"")))
                .append("\"<br>");
        if(action.equals(PolicyAction.PERMIT.name())) {
            sb.append("    set action accept").append("<br>");
        }
        sb.append("    set schedule \"always\"").append("<br>");
        sb.append("    set service \"")
                .append(service.stream()
                        .map(ServiceShortResponse::name)
                        .collect(Collectors.joining("\" \"")))
                .append("\"<br>");
        sb.append("    set logtraffic ").append(policy.isLogging() ? "all" : "disable").append("<br>");
        if (policy.isNat()) {
            sb.append("    set nat enable").append("<br>");
        }

        sb.append("next<br>");

        return sb.toString();
    }

    @Override
    public String getCommandKey() {
        return "config firewall policy";
    }

    @Override
    public String getVendorKey() {
        return AppConstants.FORTIGATE;
    }
}
