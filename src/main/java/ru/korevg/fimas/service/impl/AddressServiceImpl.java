package ru.korevg.fimas.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.korevg.fimas.dto.address.*;
import ru.korevg.fimas.entity.*;
import ru.korevg.fimas.exception.EntityExistsException;
import ru.korevg.fimas.exception.EntityNotFoundException;
import ru.korevg.fimas.mapper.AddressMapper;
import ru.korevg.fimas.repository.AddressRepository;
import ru.korevg.fimas.repository.FirewallRepository;
import ru.korevg.fimas.service.AddressService;

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
    public AddressResponse update(Long id, Object request) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Адрес не найден"));

        if (request instanceof AddressCommonCreateRequest common) {
            addressMapper.updateCommonFromRequest(common, (CommonAddress) address);
            ((CommonAddress) address).setAddresses(common.addresses());
        } else if (request instanceof AddressDynamicCreateRequest dynamic) {
            addressMapper.updateDynamicFromRequest(dynamic, (DynamicAddress) address);
            ((DynamicAddress) address).setAddresses(dynamic.addresses());

            if (dynamic.firewallId() != null) {
                Firewall fw = firewallRepository.findById(dynamic.firewallId())
                        .orElseThrow(() -> new EntityNotFoundException("Firewall не найден"));
                ((DynamicAddress) address).setFirewall(fw);
            }
        } else {
            throw new IllegalArgumentException("Неподдерживаемый тип запроса");
        }

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
}
