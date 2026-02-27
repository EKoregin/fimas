package ru.korevg.fimas.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.korevg.fimas.dto.address.AddressCommonCreateRequest;
import ru.korevg.fimas.dto.address.AddressDynamicCreateRequest;
import ru.korevg.fimas.dto.address.AddressResponse;
import ru.korevg.fimas.entity.Address;

public interface AddressService {

    AddressResponse createCommon(AddressCommonCreateRequest request);
    AddressResponse createDynamic(AddressDynamicCreateRequest request);
    AddressResponse update(Long id, Object request); // можно сделать отдельные методы
    void delete(Long id);
    AddressResponse findById(Long id);
    Page<AddressResponse> findAll(Pageable pageable);
}