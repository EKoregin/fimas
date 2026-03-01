package ru.korevg.fimas.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.korevg.fimas.dto.service.ServiceCreateRequest;
import ru.korevg.fimas.dto.service.ServiceResponse;
import ru.korevg.fimas.dto.service.ServiceShortResponse;
import ru.korevg.fimas.dto.service.ServiceUpdateRequest;

import java.util.List;

public interface ServiceService {

    ServiceResponse create(ServiceCreateRequest request);
    ServiceResponse update(Long id, ServiceUpdateRequest request);
    void delete(Long id);
    ServiceResponse findById(Long id);
    Page<ServiceResponse> findAll(Pageable pageable);

    List<ServiceShortResponse> findAllShort();

    long count();
}
