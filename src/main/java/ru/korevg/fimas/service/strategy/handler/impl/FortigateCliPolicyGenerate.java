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
    public String handle(Command command, Long firewallId) {
        log.info("Создание конфигурации CLI для политик Fortigate: {}", command.getName());
        var policies = policyService.findByFirewallId(firewallId);

        String configBody = policies.stream()
                .map(this::policyToEditBlock)
                .collect(Collectors.joining("\n"));

        log.info("Создание конфигурации завершено");

        return """
                -=Все политики по умолчанию выключены=-
                config firewall policy
                %s
                end
                Всего %s политик
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
        sb.append("edit 0\n");
        sb.append("    set status disable\n");
        sb.append("    set name \"").append(name).append("\"\n");
        sb.append("    set srcintf \"").append(srcZone).append("\"\n");
        sb.append("    set dstintf \"").append(dstZone).append("\"\n");
        sb.append("    set srcaddr \"")
                .append(srcAddr.isEmpty() ? "all" : srcAddr.stream()
                        .map(AddressShortResponse::name)
                        .collect(Collectors.joining("\" \"")))
                .append("\"\n");
        sb.append("    set dstaddr \"")
                .append(dstAddr.isEmpty() ? "all" : dstAddr.stream()
                        .map(AddressShortResponse::name)
                        .collect(Collectors.joining("\" \"")))
                .append("\"\n");
        if(action.equals(PolicyAction.PERMIT.name())) {
            sb.append("    set action accept").append("\n");
        }
        sb.append("    set schedule \"always\"").append("\n");
        sb.append("    set service \"")
                .append(service.stream()
                        .map(ServiceShortResponse::name)
                        .collect(Collectors.joining("\" \"")))
                .append("\"\n");
        sb.append("    set logtraffic disable").append("\n");
        sb.append("next");

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
