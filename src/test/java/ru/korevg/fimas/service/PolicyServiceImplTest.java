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
//import org.springframework.data.domain.Pageable;
//import ru.korevg.fimas.dto.policy.PolicyCreateRequest;
//import ru.korevg.fimas.dto.policy.PolicyResponse;
//import ru.korevg.fimas.dto.policy.PolicyUpdateRequest;
//import ru.korevg.fimas.entity.*;
//import ru.korevg.fimas.exception.EntityExistsException;
//import ru.korevg.fimas.exception.EntityNotFoundException;
//import ru.korevg.fimas.mapper.PolicyMapper;
//import ru.korevg.fimas.repository.*;
//import ru.korevg.fimas.service.impl.PolicyServiceImpl;
//
//import java.util.*;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class PolicyServiceImplTest {
//
//    @Mock
//    private PolicyRepository policyRepository;
//
//    @Mock
//    private FirewallRepository firewallRepository;
//
//    @Mock
//    private AddressRepository addressRepository;
//
//    @Mock
//    private ServiceRepository serviceRepository;
//
//    @Mock
//    private PolicyMapper policyMapper;
//
//    @InjectMocks
//    private PolicyServiceImpl policyService;
//
//    @Captor
//    private ArgumentCaptor<Policy> policyCaptor;
//
//    // ───────────────────────────────────────────────
//    // create()
//    // ───────────────────────────────────────────────
//
//    @Test
//    @DisplayName("create: имя уже существует → EntityExistsException")
//    void create_nameAlreadyExistsForFirewall_throwsEntityExistsException() {
//        var request = new PolicyCreateRequest(
//                "DuplicatePolicy", "desc", PolicyAction.PERMIT, PolicyStatus.ENABLED,
//                100L, Set.of(), Set.of(), Set.of()
//        );
//
//        when(policyRepository.existsByNameAndFirewallId("DuplicatePolicy", 100L))
//                .thenReturn(true);
//
//        assertThatThrownBy(() -> policyService.create(request))
//                .isInstanceOf(EntityExistsException.class)
//                .hasMessageContaining("Политика 'DuplicatePolicy' уже существует");
//
//        verifyNoMoreInteractions(policyRepository, firewallRepository, addressRepository, serviceRepository, policyMapper);
//    }
//
//    @Test
//    @DisplayName("create: firewall не найден → EntityNotFoundException")
//    void create_firewallNotFound_throwsEntityNotFoundException() {
//        var request = new PolicyCreateRequest(
//                "NewPolicy", null, PolicyAction.DENY, PolicyStatus.DISABLED,
//                999L, null, null, null
//        );
//
//        when(policyRepository.existsByNameAndFirewallId(any(), any())).thenReturn(false);
//        when(firewallRepository.findById(999L)).thenReturn(Optional.empty());
//
//        assertThatThrownBy(() -> policyService.create(request))
//                .isInstanceOf(EntityNotFoundException.class)
//                .hasMessageContaining("Firewall с ID 999 не найден");
//
//        verify(firewallRepository).findById(999L);
//        verifyNoMoreInteractions(policyRepository, addressRepository, serviceRepository, policyMapper);
//    }
//
//    @Test
//    @DisplayName("create: не все src адреса найдены → EntityNotFoundException")
//    void create_notAllSrcAddressesFound_throwsException() {
//        PolicyCreateRequest request = new PolicyCreateRequest(
//                "PolicyA", null, PolicyAction.PERMIT, PolicyStatus.ENABLED,
//                10L, Set.of(1L, 2L, 3L), Set.of(), Set.of()
//        );
//
//        // Создаём мок-сущность, которую вернёт маппер
//        Policy entity = new Policy();
//        entity.setName("PolicyA");
//        entity.setAction(PolicyAction.PERMIT);
//        entity.setStatus(PolicyStatus.ENABLED);
//        // srcAddresses, dstAddresses, services пока пустые — это нормально
//
//        // Настраиваем маппер
//        when(policyMapper.toEntity(request)).thenReturn(entity);
//
//        // Остальные when-ы без изменений
//        when(policyRepository.existsByNameAndFirewallId(any(), any())).thenReturn(false);
//        when(firewallRepository.findById(10L)).thenReturn(Optional.of(new Firewall()));
//        when(addressRepository.findAllById(Set.of(1L, 2L, 3L)))
//                .thenReturn(List.of(
//                        createAddress(1L, "A1"),
//                        createAddress(2L, "A2")
//                ));
//
//        assertThatThrownBy(() -> policyService.create(request))
//                .isInstanceOf(EntityNotFoundException.class)
//                .hasMessage("Не все source-адреса найдены");
//
//        // Опционально: можно проверить, что save не вызывался
//        verify(policyRepository, never()).save(any());
//    }
//
//    @Test
//    @DisplayName("create: успешное создание без коллекций")
//    void create_successNoCollections() {
//        var request = new PolicyCreateRequest(
//                "CleanPolicy", "test", PolicyAction.PERMIT, PolicyStatus.ENABLED,
//                5L, null, null, null
//        );
//
//        var firewall = Firewall.builder().id(5L).name("FW5").build();
//        var entity = Policy.builder()
//                .name("CleanPolicy")
//                .description("test")
//                .action(PolicyAction.PERMIT)
//                .status(PolicyStatus.ENABLED)
//                .firewall(firewall)
//                .build();
//
//        var saved = Policy.builder().id(42L).name("CleanPolicy").build(); // minimal
//
//        when(policyRepository.existsByNameAndFirewallId(any(), any())).thenReturn(false);
//        when(firewallRepository.findById(5L)).thenReturn(Optional.of(firewall));
//        when(policyMapper.toEntity(request)).thenReturn(entity);
//        when(policyRepository.save(any(Policy.class))).thenReturn(saved);
//        when(policyMapper.toResponse(saved)).thenReturn(
//                new PolicyResponse(42L, "CleanPolicy", null, null, null, 5L, "FW5", Set.of(), Set.of(), Set.of())
//        );
//
//        var response = policyService.create(request);
//
//        assertThat(response.id()).isEqualTo(42L);
//        assertThat(response.name()).isEqualTo("CleanPolicy");
//
//        verify(policyRepository).save(policyCaptor.capture());
//        var captured = policyCaptor.getValue();
//        assertThat(captured.getSrcAddresses()).isEmpty();
//        assertThat(captured.getDstAddresses()).isEmpty();
//        assertThat(captured.getServices()).isEmpty();
//    }
//
//    // ───────────────────────────────────────────────
//    // update()
//    // ───────────────────────────────────────────────
//
//    @Test
//    @DisplayName("update: политика не найдена → EntityNotFoundException")
//    void update_policyNotFound_throwsException() {
//        when(policyRepository.findById(777L)).thenReturn(Optional.empty());
//
//        var req = new PolicyUpdateRequest("NewName", null, null, null, null, null, null, null);
//
//        assertThatThrownBy(() -> policyService.update(777L, req))
//                .isInstanceOf(EntityNotFoundException.class);
//    }
//
//    @Test
//    @DisplayName("update: меняем firewall + очищаем и добавляем новые src адреса")
//    void update_changeFirewallAndReplaceSrcAddresses() {
//        var existing = Policy.builder()
//                .id(100L)
//                .name("Old")
//                .srcAddresses(new HashSet<>(List.of(addr(10L), addr(20L))))
//                .dstAddresses(new HashSet<>())
//                .services(new HashSet<>())
//                .firewall(fw(1L))
//                .build();
//
//        var updateReq = new PolicyUpdateRequest(
//                "Updated", null, null, null,
//                2L,                          // новый firewall
//                Set.of(30L, 40L),            // новые source
//                null,                        // dst не меняем
//                Set.of(100L)                 // новые сервисы
//        );
//
//        var newFw = fw(2L);
//        var newSrc = Set.of(addr(30L), addr(40L));
//        var newSvc = Set.of(svc(100L));
//
//        when(policyRepository.findById(100L)).thenReturn(Optional.of(existing));
//        when(firewallRepository.findById(2L)).thenReturn(Optional.of(newFw));
//        when(addressRepository.findAllById(Set.of(30L, 40L))).thenReturn(List.copyOf(newSrc));
//        when(serviceRepository.findAllById(Set.of(100L))).thenReturn(List.copyOf(newSvc));
//        when(policyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
//
//        doAnswer(inv -> {
//            Policy target = inv.getArgument(1);
//            target.setName("Updated");
//            // если в тесте проверяешь другие поля — добавь их сюда
//            return null;
//        }).when(policyMapper).updateFromRequest(updateReq, existing);
//
//        policyService.update(100L, updateReq);
//
//        // остальное без изменений
//        verify(policyRepository).save(policyCaptor.capture());
//        var saved = policyCaptor.getValue();
//
//        assertThat(saved.getName()).isEqualTo("Updated");
//        assertThat(saved.getFirewall()).isSameAs(newFw);
//        assertThat(saved.getSrcAddresses()).containsExactlyInAnyOrderElementsOf(newSrc);
//        assertThat(saved.getDstAddresses()).isEmpty();
//        assertThat(saved.getServices()).containsExactlyElementsOf(newSvc);
//    }
//
//    @Test
//    @DisplayName("update: передаём пустой список src → коллекция очищается")
//    void update_srcAddressIdsEmpty_clearsCollection() {
//        var policy = Policy.builder()
//                .id(200L)
//                .srcAddresses(new HashSet<>(List.of(addr(1L), addr(2L))))
//                .build();
//
//        var req = new PolicyUpdateRequest(null, null, null, null, null, Set.of(), null, null);
//
//        when(policyRepository.findById(200L)).thenReturn(Optional.of(policy));
//        when(policyRepository.save(any())).thenAnswer(i -> i.getArgument(0));
//
//        policyService.update(200L, req);
//
//        verify(policyRepository).save(policyCaptor.capture());
//        assertThat(policyCaptor.getValue().getSrcAddresses()).isEmpty();
//    }
//
//    // ───────────────────────────────────────────────
//    // delete / findById / findAll / findByFirewallId
//    // ───────────────────────────────────────────────
//
//    @Test
//    @DisplayName("delete: политика не существует → исключение")
//    void delete_notExists_throws() {
//        when(policyRepository.existsById(500L)).thenReturn(false);
//
//        assertThatThrownBy(() -> policyService.delete(500L))
//                .isInstanceOf(EntityNotFoundException.class);
//    }
//
//    @Test
//    @DisplayName("delete: существует → вызывается deleteById")
//    void delete_exists_callsRepository() {
//        when(policyRepository.existsById(600L)).thenReturn(true);
//
//        policyService.delete(600L);
//
//        verify(policyRepository).deleteById(600L);
//    }
//
//    @Test
//    void findById_notFound_throws() {
//        when(policyRepository.findById(700L)).thenReturn(Optional.empty());
//
//        assertThatThrownBy(() -> policyService.findById(700L))
//                .isInstanceOf(EntityNotFoundException.class);
//    }
//
//    @Test
//    void findById_success() {
//        var p = new Policy(); p.setId(800L); p.setName("Found");
//        when(policyRepository.findById(800L)).thenReturn(Optional.of(p));
//        when(policyMapper.toResponse(p)).thenReturn(new PolicyResponse(800L, "Found", null, null, null, null, null, Set.of(), Set.of(), Set.of()));
//
//        var dto = policyService.findById(800L);
//
//        assertThat(dto.id()).isEqualTo(800L);
//        assertThat(dto.name()).isEqualTo("Found");
//    }
//
//    @Test
//    void findAll_mapsPage() {
//        var p1 = Policy.builder().id(1L).name("P1").build();
//        var p2 = Policy.builder().id(2L).name("P2").build();
//
//        Page<Policy> page = new PageImpl<>(List.of(p1, p2), PageRequest.of(0, 10), 2);
//        when(policyRepository.findAll(any(Pageable.class))).thenReturn(page);
//
//        var dto1 = new PolicyResponse(1L, "P1", null, null, null, null, null, Set.of(), Set.of(), Set.of());
//        var dto2 = new PolicyResponse(2L, "P2", null, null, null, null, null, Set.of(), Set.of(), Set.of());
//        when(policyMapper.toResponse(p1)).thenReturn(dto1);
//        when(policyMapper.toResponse(p2)).thenReturn(dto2);
//
//        var result = policyService.findAll(PageRequest.of(0, 10));
//
//        assertThat(result.getContent()).containsExactly(dto1, dto2);
//    }
//
//    // ───────────────────────────────────────────────
//    // Вспомогательные методы для читаемости
//    // ───────────────────────────────────────────────
//
//    private CommonAddress createAddress(Long id, String name) {
//        CommonAddress addr = new CommonAddress();
//        addr.setId(id);
//        addr.setName(name);
//        return addr;
//    }
//
//    private Address addr(long id) {
//        var a = new CommonAddress();
//        a.setId(id);
//        a.setName("Addr" + id);
//        return a;
//    }
//
//    private Firewall fw(long id) {
//        var f = new Firewall();
//        f.setId(id);
//        f.setName("FW" + id);
//        return f;
//    }
//
//    private Service svc(long id) {
//        var s = new Service();
//        s.setId(id);
//        s.setName("Svc" + id);
//        return s;
//    }
//}