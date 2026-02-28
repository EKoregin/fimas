package ru.korevg.fimas.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.korevg.fimas.dto.port.PortCreateRequest;
import ru.korevg.fimas.dto.port.PortResponse;
import ru.korevg.fimas.dto.port.PortShortResponse;
import ru.korevg.fimas.dto.port.PortUpdateRequest;

import java.util.List;

public interface PortService {

    PortResponse create(PortCreateRequest request);
    PortResponse update(Long id, PortUpdateRequest request);
    void delete(Long id);
    PortResponse findById(Long id);
    Page<PortResponse> findAll(Pageable pageable);

    long count();

    List<PortShortResponse> findAllShort();
}