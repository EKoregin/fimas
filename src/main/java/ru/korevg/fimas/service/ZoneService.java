package ru.korevg.fimas.service;

import ru.korevg.fimas.dto.zone.ZoneResponse;

import java.util.List;

public interface ZoneService {

    List<ZoneResponse> findAll();
    ZoneResponse findById(Long id);
    ZoneResponse findByName(String name);
    ZoneResponse getAnyZone();
}
