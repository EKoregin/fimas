package ru.korevg.fimas.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import ru.korevg.fimas.dto.policy.PolicyCreateRequest;
import ru.korevg.fimas.dto.policy.PolicyResponse;
import ru.korevg.fimas.dto.policy.PolicyUpdateRequest;
import ru.korevg.fimas.entity.*;
import ru.korevg.fimas.exception.EntityExistsException;
import ru.korevg.fimas.exception.EntityNotFoundException;
import ru.korevg.fimas.mapper.PolicyMapper;
import ru.korevg.fimas.repository.*;
import ru.korevg.fimas.service.PolicyService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyServiceImpl implements PolicyService {

    private final PolicyRepository policyRepository;
    private final FirewallRepository firewallRepository;
    private final AddressRepository addressRepository;
    private final ServiceRepository serviceRepository;
    private final PolicyMapper policyMapper;

    @Override
    @Transactional
    public PolicyResponse create(PolicyCreateRequest request) {
        if (policyRepository.existsByNameAndFirewallId(request.name(), request.firewallId())) {
            throw new EntityExistsException(
                    "Политика '" + request.name() + "' уже существует для этого firewall"
            );
        }

        Firewall firewall = firewallRepository.findById(request.firewallId())
                .orElseThrow(() -> new EntityNotFoundException("Firewall с ID " + request.firewallId() + " не найден"));

        Policy policy = policyMapper.toEntity(request);
        policy.setFirewall(firewall);

        // Загружаем и привязываем адреса источника
        if (request.srcAddressIds() != null && !request.srcAddressIds().isEmpty()) {
            Set<Address> srcAddresses = addressRepository.findAllById(request.srcAddressIds())
                    .stream().collect(Collectors.toSet());
            if (srcAddresses.size() != request.srcAddressIds().size()) {
                throw new EntityNotFoundException("Не все source-адреса найдены");
            }
            policy.setSrcAddresses(srcAddresses);
        }

        // Загружаем и привязываем адреса назначения
        if (request.dstAddressIds() != null && !request.dstAddressIds().isEmpty()) {
            Set<Address> dstAddresses = addressRepository.findAllById(request.dstAddressIds())
                    .stream().collect(Collectors.toSet());
            if (dstAddresses.size() != request.dstAddressIds().size()) {
                throw new EntityNotFoundException("Не все destination-адреса найдены");
            }
            policy.setDstAddresses(dstAddresses);
        }

        // Загружаем и привязываем сервисы
        if (request.serviceIds() != null && !request.serviceIds().isEmpty()) {
            Set<Service> services = serviceRepository.findAllById(request.serviceIds())
                    .stream().collect(Collectors.toSet());
            if (services.size() != request.serviceIds().size()) {
                throw new EntityNotFoundException("Не все сервисы найдены");
            }
            policy.setServices(services);
        }

        Policy saved = policyRepository.save(policy);
        return policyMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PolicyResponse update(Long id, PolicyUpdateRequest request) {
        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Политика с ID " + id + " не найдена"));

        policyMapper.updateFromRequest(request, policy);

        // Обновление firewall, если передан
        if (request.firewallId() != null) {
            Firewall newFirewall = firewallRepository.findById(request.firewallId())
                    .orElseThrow(() -> new EntityNotFoundException("Firewall с ID " + request.firewallId() + " не найден"));
            policy.setFirewall(newFirewall);
        }

        // Обновление source-адресов (замена всего множества)
        if (request.srcAddressIds() != null) {
            Set<Address> newSrc = new HashSet<>();
            if (!request.srcAddressIds().isEmpty()) {
                newSrc = addressRepository.findAllById(request.srcAddressIds())
                        .stream().collect(Collectors.toSet());
                if (newSrc.size() != request.srcAddressIds().size()) {
                    throw new EntityNotFoundException("Не все source-адреса найдены");
                }
            }
            policy.getSrcAddresses().clear();
            policy.getSrcAddresses().addAll(newSrc);
        }

        // Обновление destination-адресов
        if (request.dstAddressIds() != null) {
            Set<Address> newDst = new HashSet<>();
            if (!request.dstAddressIds().isEmpty()) {
                newDst = addressRepository.findAllById(request.dstAddressIds())
                        .stream().collect(Collectors.toSet());
                if (newDst.size() != request.dstAddressIds().size()) {
                    throw new EntityNotFoundException("Не все destination-адреса найдены");
                }
            }
            policy.getDstAddresses().clear();
            policy.getDstAddresses().addAll(newDst);
        }

        // Обновление сервисов
        if (request.serviceIds() != null) {
            Set<Service> newServices = new HashSet<>();
            if (!request.serviceIds().isEmpty()) {
                newServices = serviceRepository.findAllById(request.serviceIds())
                        .stream().collect(Collectors.toSet());
                if (newServices.size() != request.serviceIds().size()) {
                    throw new EntityNotFoundException("Не все сервисы найдены");
                }
            }
            policy.getServices().clear();
            policy.getServices().addAll(newServices);
        }

        Policy updated = policyRepository.save(policy);
        return policyMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!policyRepository.existsById(id)) {
            throw new EntityNotFoundException("Политика с ID " + id + " не найдена");
        }
        policyRepository.deleteById(id);
    }

    @Override
    public PolicyResponse findById(Long id) {
        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Политика с ID " + id + " не найдена"));
        return policyMapper.toResponse(policy);
    }

    @Override
    public Page<PolicyResponse> findAll(Pageable pageable) {
        return policyRepository.findAll(pageable)
                .map(policyMapper::toResponse);
    }

    @Override
    public List<PolicyResponse> findAll() {
        return policyRepository.findAll().stream().map(policyMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    public Page<PolicyResponse> findByFirewallId(Long firewallId, Pageable pageable) {
        return policyRepository.findByFirewallId(firewallId, pageable)
                .map(policyMapper::toResponse);
    }

    @Override
    public long countByFirewallId(Long firewallId) {
        return policyRepository.countByFirewallId(firewallId);
    }

    @Override
    public long count() {
        return policyRepository.count();
    }
}