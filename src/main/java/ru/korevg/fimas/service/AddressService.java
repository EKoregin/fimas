package ru.korevg.fimas.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.korevg.fimas.dto.address.AddressCommonCreateRequest;
import ru.korevg.fimas.dto.address.AddressDynamicCreateRequest;
import ru.korevg.fimas.dto.address.AddressResponse;

public interface AddressService {

    AddressResponse createCommon(AddressCommonCreateRequest request);
    AddressResponse createDynamic(AddressDynamicCreateRequest request);
    AddressResponse updateCommon(Long id, AddressCommonCreateRequest request);
    AddressResponse updateDynamic(Long id, AddressDynamicCreateRequest request);
    void delete(Long id);
    AddressResponse findById(Long id);
    Page<AddressResponse> findAll(Pageable pageable);
}