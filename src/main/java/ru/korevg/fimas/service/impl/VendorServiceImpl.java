package ru.korevg.fimas.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.korevg.fimas.dto.vendor.VendorCreateRequest;
import ru.korevg.fimas.dto.vendor.VendorResponse;
import ru.korevg.fimas.dto.vendor.VendorUpdateRequest;
import ru.korevg.fimas.entity.Vendor;
import ru.korevg.fimas.exception.EntityExistsException;
import ru.korevg.fimas.exception.EntityNotFoundException;
import ru.korevg.fimas.mapper.VendorMapper;
import ru.korevg.fimas.repository.VendorRepository;
import ru.korevg.fimas.service.VendorService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendorServiceImpl implements VendorService {

    private final VendorRepository vendorRepository;
    private final VendorMapper vendorMapper;

    @Override
    @Transactional
    public VendorResponse create(VendorCreateRequest request) {
        if (vendorRepository.existsByName(request.name())) {
            throw new EntityExistsException("Вендор с именем '" + request.name() + "' уже существует");
        }

        Vendor vendor = vendorMapper.toEntity(request);
        Vendor saved = vendorRepository.save(vendor);
        return vendorMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public VendorResponse update(Long id, VendorUpdateRequest request) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Вендор с ID " + id + " не найден"));

        vendorMapper.updateFromRequest(request, vendor);

        Vendor updated = vendorRepository.save(vendor);
        return vendorMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!vendorRepository.existsById(id)) {
            throw new EntityNotFoundException("Вендор с ID " + id + " не найден");
        }
        vendorRepository.deleteById(id);
    }

    @Override
    public VendorResponse findById(Long id) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Вендор с ID " + id + " не найден"));
        return vendorMapper.toResponse(vendor);
    }

    @Override
    public Page<VendorResponse> findAll(Pageable pageable) {
        return vendorRepository.findAll(pageable)
                .map(vendorMapper::toResponse);
    }
}