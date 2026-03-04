package ru.korevg.fimas.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.korevg.fimas.dto.address.AddressCommonCreateRequest;
import ru.korevg.fimas.dto.address.AddressDynamicCreateRequest;
import ru.korevg.fimas.dto.address.AddressResponse;
import ru.korevg.fimas.dto.address.AddressShortResponse;
import ru.korevg.fimas.entity.Address;
import ru.korevg.fimas.entity.CommonAddress;
import ru.korevg.fimas.entity.DynamicAddress;
import ru.korevg.fimas.entity.Firewall;
import ru.korevg.fimas.exception.EntityExistsException;
import ru.korevg.fimas.exception.EntityNotFoundException;
import ru.korevg.fimas.mapper.AddressMapper;
import ru.korevg.fimas.repository.AddressRepository;
import ru.korevg.fimas.repository.FirewallRepository;
import ru.korevg.fimas.service.AddressService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final FirewallRepository firewallRepository;
    private final AddressMapper addressMapper;

    @Override
    @Transactional
    public AddressResponse createCommon(AddressCommonCreateRequest request) {
        if (addressRepository.existsByName(request.name())) {
            throw new EntityExistsException("Адрес с именем '" + request.name() + "' уже существует");
        }

        CommonAddress address = addressMapper.toCommonEntity(request);
        address.setAddresses(request.addresses());

        Address saved = addressRepository.save(address);
        return addressMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AddressResponse createDynamic(AddressDynamicCreateRequest request) {
        if (addressRepository.existsByName(request.name())) {
            throw new EntityExistsException("Адрес с именем '" + request.name() + "' уже существует");
        }

        Firewall firewall = firewallRepository.findById(request.firewallId())
                .orElseThrow(() -> new EntityNotFoundException("Firewall не найден"));

        DynamicAddress address = addressMapper.toDynamicEntity(request);
        address.setAddresses(request.addresses());
        address.setFirewall(firewall);

        Address saved = addressRepository.save(address);
        return addressMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AddressResponse updateCommon(Long id, AddressCommonCreateRequest request) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Адрес не найден"));

        addressMapper.updateCommonFromRequest(request, (CommonAddress) address);
        address.setAddresses(address.getAddresses());


        Address updated = addressRepository.save(address);
        return addressMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public AddressResponse updateDynamic(Long id, AddressDynamicCreateRequest request) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Адрес не найден"));

        addressMapper.updateDynamicFromRequest(request, (DynamicAddress) address);
        address.setAddresses(address.getAddresses());


        Address updated = addressRepository.save(address);
        return addressMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!addressRepository.existsById(id)) {
            throw new EntityNotFoundException("Адрес не найден");
        }
        addressRepository.deleteById(id);
        log.info("Service with ID: {} deleted.", id);
    }

    @Override
    public AddressResponse findById(Long id) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Адрес не найден"));
        return addressMapper.toResponse(address);
    }

    @Override
    public Page<AddressResponse> findAll(Pageable pageable) {
        return addressRepository.findAll(pageable)
                .map(addressMapper::toResponse);
    }

    @Override
    public long count() {
        return addressRepository.count();
    }

    @Override
    public Page<AddressResponse> findAll(Pageable pageable, String search) {
        if (search == null || search.trim().isEmpty()) {
            return addressRepository.findAll(pageable)
                    .map(addressMapper::toResponse);
        }

        String pattern = "%" + search.trim().toLowerCase() + "%";

        Page<Address> page = addressRepository.findBySearchPattern(pattern, pageable);
        return page.map(addressMapper::toResponse);
    }

    @Override
    public long count(String search) {
        if (search == null || search.trim().isEmpty()) {
            return addressRepository.count();
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return addressRepository.countBySearchPattern(pattern);
    }

    @Override
    public List<AddressShortResponse> findAllShort() {
        return addressRepository.findAllShort();
    }

    @Override
    public List<AddressShortResponse> findAllShort(Long firewallId) {
        return addressRepository.findAllShort(firewallId);
    }
}
