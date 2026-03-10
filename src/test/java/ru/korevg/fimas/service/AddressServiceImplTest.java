//package ru.korevg.fimas.service;
//
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.Captor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.PageRequest;
//import ru.korevg.fimas.dto.address.AddressCommonCreateRequest;
//import ru.korevg.fimas.dto.address.AddressDynamicCreateRequest;
//import ru.korevg.fimas.dto.address.AddressResponse;
//import ru.korevg.fimas.entity.Address;
//import ru.korevg.fimas.entity.AddressSubType;
//import ru.korevg.fimas.entity.CommonAddress;
//import ru.korevg.fimas.entity.DynamicAddress;
//import ru.korevg.fimas.entity.Firewall;
//import ru.korevg.fimas.exception.EntityExistsException;
//import ru.korevg.fimas.exception.EntityNotFoundException;
//import ru.korevg.fimas.mapper.AddressMapper;
//import ru.korevg.fimas.repository.AddressRepository;
//import ru.korevg.fimas.repository.FirewallRepository;
//import ru.korevg.fimas.service.impl.AddressServiceImpl;
//
//import java.util.List;
//import java.util.Optional;
//import java.util.Set;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class AddressServiceImplTest {
//
//    @Mock
//    private AddressRepository addressRepository;
//
//    @Mock
//    private FirewallRepository firewallRepository;
//
//    @Mock
//    private AddressMapper addressMapper;
//
//    @InjectMocks
//    private AddressServiceImpl addressService;
//
//    @Captor
//    private ArgumentCaptor<Address> addressCaptor;
//
//    // ───────────────────────────────────────────────
//    // createCommon
//    // ───────────────────────────────────────────────
//
//    @Test
//    @DisplayName("createCommon → имя уже существует → EntityExistsException")
//    void createCommon_nameExists_throwsEntityExistsException() {
//        var request = new AddressCommonCreateRequest("Duplicate", "desc", Set.of("10.0.0.1"));
//
//        when(addressRepository.existsByName("Duplicate")).thenReturn(true);
//
//        assertThatThrownBy(() -> addressService.createCommon(request))
//                .isInstanceOf(EntityExistsException.class)
//                .hasMessageContaining("Адрес с именем 'Duplicate' уже существует");
//
//        verifyNoMoreInteractions(addressRepository, firewallRepository, addressMapper);
//    }
//
//    @Test
//    @DisplayName("createCommon → успешное создание")
//    void createCommon_success() {
//        var request = new AddressCommonCreateRequest(
//                "OfficeNet", "Офисная сеть", Set.of("192.168.10.0/24", "10.5.0.0/16")
//        );
//
//        var entity = new CommonAddress();
//        entity.setName("OfficeNet");
//        entity.setDescription("Офисная сеть");
//
//        var saved = new CommonAddress();
//        saved.setId(42L);
//        saved.setName("OfficeNet");
//        saved.setDescription("Офисная сеть");
//        saved.setAddresses(Set.of("192.168.10.0/24", "10.5.0.0/16"));
//
//        when(addressRepository.existsByName(anyString())).thenReturn(false);
//        when(addressMapper.toCommonEntity(request)).thenReturn(entity);
//        when(addressRepository.save(any())).thenReturn(saved);
//        when(addressMapper.toResponse(saved)).thenReturn(
//                new AddressResponse(42L, "COMMON", "OfficeNet", "Офисная сеть",
//                        Set.of("192.168.10.0/24", "10.5.0.0/16"), null, null)
//        );
//
//        var response = addressService.createCommon(request);
//
//        assertThat(response.id()).isEqualTo(42L);
//        assertThat(response.addressType()).isEqualTo("COMMON");
//        assertThat(response.name()).isEqualTo("OfficeNet");
//        assertThat(response.addresses()).containsExactlyInAnyOrder("192.168.10.0/24", "10.5.0.0/16");
//
//        verify(addressRepository).save(addressCaptor.capture());
//        var captured = addressCaptor.getValue();
//        assertThat(captured.getAddresses()).containsExactlyInAnyOrder("192.168.10.0/24", "10.5.0.0/16");
//    }
//
//    // ───────────────────────────────────────────────
//    // createDynamic
//    // ───────────────────────────────────────────────
//
//    @Test
//    @DisplayName("createDynamic → firewall не найден → EntityNotFoundException")
//    void createDynamic_firewallNotFound_throws() {
//        var request = AddressDynamicCreateRequest.builder()
//                .name("Dyn1")
//                .addresses(Set.of("8.8.8.8"))
//                .subType(AddressSubType.IP.name())
//                .firewallId(999L)
//                .build();
//
//        when(addressRepository.existsByName(any())).thenReturn(false);
//        when(firewallRepository.findById(999L)).thenReturn(Optional.empty());
//
//        assertThatThrownBy(() -> addressService.createDynamic(request))
//                .isInstanceOf(EntityNotFoundException.class)
//                .hasMessageContaining("Firewall не найден");
//    }
//
//    @Test
//    @DisplayName("createDynamic → успешное создание")
//    void createDynamic_success() {
//
//        var request = AddressDynamicCreateRequest.builder()
//                .name("GoogleDNS")
//                .addresses(Set.of("8.8.8.8", "8.8.4.4"))
//                .subType(AddressSubType.IP.name())
//                .firewallId(5L)
//                .build();
//
//        var firewall = new Firewall();
//        firewall.setId(5L);
//        firewall.setName("MainFW");
//
//        var entity = new DynamicAddress();
//        entity.setName("GoogleDNS");
//        entity.setDescription("DNS Google");
//
//        var saved = new DynamicAddress();
//        saved.setId(77L);
//        saved.setName("GoogleDNS");
//        saved.setDescription("DNS Google");
//        saved.setAddresses(Set.of("8.8.8.8", "8.8.4.4"));
//        saved.setFirewall(firewall);
//
//        when(addressRepository.existsByName(any())).thenReturn(false);
//        when(firewallRepository.findById(5L)).thenReturn(Optional.of(firewall));
//        when(addressMapper.toDynamicEntity(request)).thenReturn(entity);
//        when(addressRepository.save(any())).thenReturn(saved);
//        when(addressMapper.toResponse(saved)).thenReturn(
//                new AddressResponse(77L, "DYNAMIC", "GoogleDNS", "DNS Google",
//                        Set.of("8.8.8.8", "8.8.4.4"), 5L, "MainFW")
//        );
//
//        var response = addressService.createDynamic(request);
//
//        assertThat(response.addressType()).isEqualTo("DYNAMIC");
//        assertThat(response.firewallId()).isEqualTo(5L);
//        assertThat(response.firewallName()).isEqualTo("MainFW");
//    }
//
//    // ───────────────────────────────────────────────
//    // updateCommon
//    // ───────────────────────────────────────────────
//
//    @Test
//    @DisplayName("updateCommon → адрес не найден → EntityNotFoundException")
//    void updateCommon_notFound_throws() {
//        when(addressRepository.findById(100L)).thenReturn(Optional.empty());
//
//        var req = new AddressCommonCreateRequest("NewName", null, Set.of("1.1.1.1"));
//
//        assertThatThrownBy(() -> addressService.updateCommon(100L, req))
//                .isInstanceOf(EntityNotFoundException.class);
//    }
//
//    @Test
//    @DisplayName("updateCommon → успешное обновление")
//    void updateCommon_success() {
//        var existing = new CommonAddress();
//        existing.setId(200L);
//        existing.setName("OldName");
//        existing.setAddresses(Set.of("172.16.0.0/16"));
//
//        var request = new AddressCommonCreateRequest(
//                "UpdatedName", "Новое описание", Set.of("10.10.10.0/24")
//        );
//
//        var updatedEntity = new CommonAddress();
//        updatedEntity.setId(200L);
//        updatedEntity.setName("UpdatedName");
//        updatedEntity.setDescription("Новое описание");
//        updatedEntity.setAddresses(Set.of("10.10.10.0/24"));
//
//        when(addressRepository.findById(200L)).thenReturn(Optional.of(existing));
//        doNothing().when(addressMapper).updateCommonFromRequest(request, existing);
//        when(addressRepository.save(any())).thenReturn(updatedEntity);
//        when(addressMapper.toResponse(updatedEntity)).thenReturn(
//                new AddressResponse(200L, "COMMON", "UpdatedName", "Новое описание",
//                        Set.of("10.10.10.0/24"), null, null)
//        );
//
//        var response = addressService.updateCommon(200L, request);
//
//        assertThat(response.name()).isEqualTo("UpdatedName");
//        assertThat(response.description()).isEqualTo("Новое описание");
//        assertThat(response.addresses()).containsExactly("10.10.10.0/24");
//    }
//
//    // ───────────────────────────────────────────────
//    // delete / findById / findAll
//    // ───────────────────────────────────────────────
//
//    @Test
//    void delete_notExists_throws() {
//        when(addressRepository.existsById(300L)).thenReturn(false);
//
//        assertThatThrownBy(() -> addressService.delete(300L))
//                .isInstanceOf(EntityNotFoundException.class);
//    }
//
//    @Test
//    void delete_exists_callsDelete() {
//        when(addressRepository.existsById(400L)).thenReturn(true);
//
//        addressService.delete(400L);
//
//        verify(addressRepository).deleteById(400L);
//    }
//
//    @Test
//    void findById_notFound_throws() {
//        when(addressRepository.findById(500L)).thenReturn(Optional.empty());
//
//        assertThatThrownBy(() -> addressService.findById(500L))
//                .isInstanceOf(EntityNotFoundException.class);
//    }
//
//    @Test
//    void findAll_returnsMappedPage() {
//        var addr1 = new CommonAddress();
//        addr1.setId(1L);
//        addr1.setName("Addr1");
//
//        var addr2 = new DynamicAddress();
//        addr2.setId(2L);
//        addr2.setName("Addr2");
//
//        Page<Address> page = new PageImpl<>(List.of(addr1, addr2), PageRequest.of(0, 20), 2);
//
//        when(addressRepository.findAll(any(PageRequest.class))).thenReturn(page);
//
//        when(addressMapper.toResponse(addr1)).thenReturn(
//                new AddressResponse(1L, "COMMON", "Addr1", null, Set.of(), null, null)
//        );
//        when(addressMapper.toResponse(addr2)).thenReturn(
//                new AddressResponse(2L, "DYNAMIC", "Addr2", null, Set.of(), 10L, "FW10")
//        );
//
//        var result = addressService.findAll(PageRequest.of(0, 20));
//
//        assertThat(result.getContent()).hasSize(2);
//        assertThat(result.getContent().get(0).addressType()).isEqualTo("COMMON");
//        assertThat(result.getContent().get(1).addressType()).isEqualTo("DYNAMIC");
//    }
//
//    // ───────────────────────────────────────────────
//    // Дополнительно рекомендуется добавить:
//    // ───────────────────────────────────────────────
//    // • updateDynamic — аналогично updateCommon
//    // • createCommon / createDynamic с пустым Set<String> addresses
//    // • createCommon / createDynamic с name = null или пустым (но это обычно ловит валидация)
//    // • update → когда request.addresses = null → должно остаться старое значение?
//}