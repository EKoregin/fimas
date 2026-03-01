package ru.korevg.fimas.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.korevg.fimas.dto.address.AddressCommonCreateRequest;
import ru.korevg.fimas.dto.address.AddressDynamicCreateRequest;
import ru.korevg.fimas.dto.address.AddressResponse;
import ru.korevg.fimas.dto.address.AddressShortResponse;

import java.util.List;

public interface AddressService {

    AddressResponse createCommon(AddressCommonCreateRequest request);
    AddressResponse createDynamic(AddressDynamicCreateRequest request);
    AddressResponse updateCommon(Long id, AddressCommonCreateRequest request);
    AddressResponse updateDynamic(Long id, AddressDynamicCreateRequest request);
    void delete(Long id);
    AddressResponse findById(Long id);
    Page<AddressResponse> findAll(Pageable pageable);
    Page<AddressResponse> findAll(Pageable pageable, String search);  // search может быть null

    long count();
    long count(String search);

    List<AddressShortResponse> findAllShort();
}