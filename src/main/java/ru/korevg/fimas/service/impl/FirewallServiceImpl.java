package ru.korevg.fimas.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import ru.korevg.fimas.dto.firewall.FirewallCreateRequest;
import ru.korevg.fimas.dto.firewall.FirewallResponse;
import ru.korevg.fimas.dto.firewall.FirewallUpdateRequest;
import ru.korevg.fimas.entity.Address;
import ru.korevg.fimas.entity.CommonAddress;
import ru.korevg.fimas.entity.Firewall;
import ru.korevg.fimas.entity.Model;
import ru.korevg.fimas.entity.Policy;
import ru.korevg.fimas.entity.Service;
import ru.korevg.fimas.exception.EntityExistsException;
import ru.korevg.fimas.exception.EntityNotFoundException;
import ru.korevg.fimas.mapper.FirewallMapper;
import ru.korevg.fimas.repository.FirewallRepository;
import ru.korevg.fimas.repository.ModelRepository;
import ru.korevg.fimas.repository.PolicyRepository;
import ru.korevg.fimas.service.FirewallService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FirewallServiceImpl implements FirewallService {

    private final FirewallRepository firewallRepository;
    private final ModelRepository modelRepository;
    private final FirewallMapper firewallMapper;
    private final PolicyRepository policyRepository;

    @Override
    @Transactional
    public FirewallResponse create(FirewallCreateRequest request) {
        if (firewallRepository.existsByName(request.name())) {
            throw new EntityExistsException("Firewall с именем '" + request.name() + "' уже существует");
        }

        Model model = modelRepository.findById(request.modelId())
                .orElseThrow(() -> new EntityNotFoundException("Модель с ID " + request.modelId() + " не найдена"));

        Firewall firewall = firewallMapper.toEntity(request);
        firewall.setModel(model);

        Firewall saved = firewallRepository.save(firewall);
        return firewallMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public FirewallResponse update(Long id, FirewallUpdateRequest request) {
        Firewall firewall = firewallRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Firewall с ID " + id + " не найден"));

        firewallMapper.updateFromRequest(request, firewall);

        // Если передан новый modelId — обновляем модель
        if (request.modelId() != null) {
            Model newModel = modelRepository.findById(request.modelId())
                    .orElseThrow(() -> new EntityNotFoundException("Модель с ID " + request.modelId() + " не найдена"));
            firewall.setModel(newModel);
        }

        Firewall updated = firewallRepository.save(firewall);
        return firewallMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!firewallRepository.existsById(id)) {
            throw new EntityNotFoundException("Firewall с ID " + id + " не найден");
        }
        firewallRepository.deleteById(id);
    }

    @Override
    public Optional<FirewallResponse> findById(Long id) {
        return firewallRepository.findById(id)
                .map(firewallMapper::toResponse);
    }

    @Override
    public Page<FirewallResponse> findAll(Pageable pageable) {
        return firewallRepository.findAll(pageable)
                .map(firewallMapper::toResponse);
    }

    @Override
    public List<FirewallResponse> findAll() {
        return firewallRepository.findAll()
                .stream().map(firewallMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return firewallRepository.count();
    }

    @Override
    @Transactional
    public void copyPolicies(Long sourceFirewallId, Long targetFirewallId) {
        Firewall sourceFirewall = firewallRepository.findById(sourceFirewallId)
                .orElseThrow(() -> new EntityNotFoundException("Source Firewall not found"));
        Firewall targetFirewall = firewallRepository.findById(targetFirewallId)
                .orElseThrow(() -> new EntityNotFoundException("Target Firewall not found"));

        for (Policy sourcePolicy : sourceFirewall.getPolicies()) {
            Policy newPolicy = Policy.builder()
                    .name(sourcePolicy.getName())
                    .description(sourcePolicy.getDescription())
                    .srcZone(sourcePolicy.getSrcZone())
                    .dstZone(sourcePolicy.getDstZone())
                    .action(sourcePolicy.getAction())
                    .status(sourcePolicy.getStatus())
                    .isLogging(sourcePolicy.getIsLogging())
                    .isNat(sourcePolicy.getIsNat())
                    .policyOrder(sourcePolicy.getPolicyOrder())
                    .firewall(targetFirewall)
                    .build();

            for (Address srcAddress : sourcePolicy.getSrcAddresses()) {
                if (srcAddress instanceof CommonAddress) {
                    newPolicy.getSrcAddresses().add(srcAddress);
                }
            }
            for (Address dstAddress : sourcePolicy.getDstAddresses()) {
                if (dstAddress instanceof CommonAddress) {
                    newPolicy.getDstAddresses().add(dstAddress);
                }
            }
            for (Service service : sourcePolicy.getServices()) {
                newPolicy.getServices().add(service);
            }

            policyRepository.save(newPolicy);
        }
    }
}