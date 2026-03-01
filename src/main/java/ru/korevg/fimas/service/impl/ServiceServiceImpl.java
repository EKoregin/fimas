package ru.korevg.fimas.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import ru.korevg.fimas.dto.service.ServiceCreateRequest;
import ru.korevg.fimas.dto.service.ServiceResponse;
import ru.korevg.fimas.dto.service.ServiceShortResponse;
import ru.korevg.fimas.dto.service.ServiceUpdateRequest;
import ru.korevg.fimas.entity.Port;
import ru.korevg.fimas.entity.Service;
import ru.korevg.fimas.exception.EntityExistsException;
import ru.korevg.fimas.exception.EntityNotFoundException;
import ru.korevg.fimas.mapper.ServiceMapper;
import ru.korevg.fimas.repository.PortRepository;
import ru.korevg.fimas.repository.ServiceRepository;
import ru.korevg.fimas.service.ServiceService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceServiceImpl implements ServiceService {

    private final ServiceRepository serviceRepository;
    private final PortRepository portRepository;
    private final ServiceMapper serviceMapper;

    @Override
    @Transactional
    public ServiceResponse create(ServiceCreateRequest request) {
        if (serviceRepository.existsByName(request.name())) {
            throw new EntityExistsException("Сервис с именем '" + request.name() + "' уже существует");
        }

        Service service = serviceMapper.toEntity(request);

        if (request.portIds() != null && !request.portIds().isEmpty()) {
            Set<Port> ports = portRepository.findAllById(request.portIds())
                    .stream().collect(Collectors.toSet());
            if (ports.size() != request.portIds().size()) {
                throw new EntityNotFoundException("Не все порты найдены");
            }
            service.setPorts(ports);
        }

        Service saved = serviceRepository.save(service);
        return serviceMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ServiceResponse update(Long id, ServiceUpdateRequest request) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Сервис с ID " + id + " не найден"));

        serviceMapper.updateFromRequest(request, service);

        if (request.portIds() != null) {
            Set<Port> newPorts = new HashSet<>();
            if (!request.portIds().isEmpty()) {
                newPorts = portRepository.findAllById(request.portIds())
                        .stream().collect(Collectors.toSet());
                if (newPorts.size() != request.portIds().size()) {
                    throw new EntityNotFoundException("Не все порты найдены");
                }
            }
            service.getPorts().clear();
            service.getPorts().addAll(newPorts);
        }

        Service updated = serviceRepository.save(service);
        return serviceMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!serviceRepository.existsById(id)) {
            throw new EntityNotFoundException("Сервис с ID " + id + " не найден");
        }
        serviceRepository.deleteById(id);
        log.info("Service with ID {} was deleted", id);
    }

    @Override
    public ServiceResponse findById(Long id) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Сервис с ID " + id + " не найден"));
        return serviceMapper.toResponse(service);
    }

    @Override
    public Page<ServiceResponse> findAll(Pageable pageable) {
        return serviceRepository.findAll(pageable)
                .map(serviceMapper::toResponse);
    }

    @Override
    public List<ServiceShortResponse> findAllShort() {
        return serviceRepository.findAllShort();
    }

    @Override
    public long count() {
        return serviceRepository.count();
    }
}