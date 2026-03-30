package ru.korevg.fimas.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.korevg.fimas.dto.zone.ZoneResponse;
import ru.korevg.fimas.entity.Zone;
import ru.korevg.fimas.mapper.ZoneMapper;
import ru.korevg.fimas.repository.ZoneRepository;
import ru.korevg.fimas.service.ZoneService;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ZoneServiceImpl implements ZoneService {

    private final ZoneRepository zoneRepository;
    private final ZoneMapper zoneMapper;

    @Override
    public List<ZoneResponse> findAll() {
        return zoneRepository.findAllByOrderByPriorityAsc()
                .stream()
                .map(zoneMapper::toResponse)
                .toList();
    }

    @Override
    public ZoneResponse findById(Long id) {
        Zone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Зона с ID " + id + " не найдена"));
        return zoneMapper.toResponse(zone);
    }

    @Override
    public ZoneResponse findByName(String name) {
        Zone zone = zoneRepository.findByName(name)
                .orElseThrow(() -> new EntityNotFoundException("Зона с именем '" + name + "' не найдена"));
        return zoneMapper.toResponse(zone);
    }

    @Override
    public ZoneResponse getAnyZone() {
        Zone zone = zoneRepository.findByName("ANY")
                .orElseThrow(() -> new EntityNotFoundException("Зона ANY не найдена в базе данных"));
        return zoneMapper.toResponse(zone);
    }
}
