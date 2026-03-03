package ru.korevg.fimas.service.impl;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.korevg.fimas.dto.port.PortCreateRequest;
import ru.korevg.fimas.dto.port.PortResponse;
import ru.korevg.fimas.dto.port.PortShortResponse;
import ru.korevg.fimas.dto.port.PortUpdateRequest;
import ru.korevg.fimas.entity.Port;
import ru.korevg.fimas.entity.Protocol;
import ru.korevg.fimas.exception.EntityExistsException;
import ru.korevg.fimas.exception.EntityNotFoundException;
import ru.korevg.fimas.mapper.PortMapper;
import ru.korevg.fimas.repository.PortRepository;
import ru.korevg.fimas.service.PortService;
import ru.korevg.fimas.validation.ValidPortRange;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortServiceImpl implements PortService {

    private final PortRepository portRepository;
    private final PortMapper portMapper;

    @Override
    @Transactional
    public PortResponse create(PortCreateRequest request) {
        if (portRepository.existsByProtocolAndDstPort(
                Protocol.valueOf(request.protocol()), request.dstPort())) {
            throw new EntityExistsException(
                    "Порт " + request.protocol() + "/" + request.dstPort() + " уже существует");
        }

        Port port = portMapper.toEntity(request);
        Port saved = portRepository.save(port);
        return portMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PortResponse update(Long id, PortUpdateRequest request) {
        Port port = portRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Порт с ID " + id + " не найден"));

        portMapper.updateFromRequest(request, port);

        Port updated = portRepository.save(port);
        return portMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!portRepository.existsById(id)) {
            throw new EntityNotFoundException("Порт с ID " + id + " не найден");
        }
        portRepository.deleteById(id);
    }

    @Override
    public PortResponse findById(Long id) {
        Port port = portRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Порт с ID " + id + " не найден"));
        return portMapper.toResponse(port);
    }

    @Override
    public Page<PortResponse> findAll(Pageable pageable) {
        return portRepository.findAll(pageable)
                .map(portMapper::toResponse);
    }

    @Override
    public long count() {
        return portRepository.count();
    }

    @Override
    public List<PortShortResponse> findAllShort() {
        return portRepository.findAllShort().stream()
                .map(p -> new PortShortResponse(p.getId(), p.getProtocol(), p.getSrcPort(), p.getDstPort()))
                .collect(Collectors.toList());
    }
}
