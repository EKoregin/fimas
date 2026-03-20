package ru.korevg.fimas.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.korevg.fimas.dto.vendor.VendorCreateRequest;
import ru.korevg.fimas.dto.vendor.VendorResponse;
import ru.korevg.fimas.dto.vendor.VendorUpdateRequest;

import java.util.List;
import java.util.Optional;

public interface VendorService {

    VendorResponse create(VendorCreateRequest request);

    VendorResponse update(Long id, VendorUpdateRequest request);

    void delete(Long id);

    VendorResponse findById(Long id);

    Optional<VendorResponse> findByIdOptional(Long id);

    Page<VendorResponse> findAll(Pageable pageable);

    List<VendorResponse> findAll();
}