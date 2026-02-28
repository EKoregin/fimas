package ru.korevg.fimas.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.korevg.fimas.dto.firewall.FirewallCreateRequest;
import ru.korevg.fimas.dto.firewall.FirewallResponse;
import ru.korevg.fimas.dto.firewall.FirewallUpdateRequest;
import ru.korevg.fimas.entity.Firewall;
import ru.korevg.fimas.entity.Model;
import ru.korevg.fimas.entity.Vendor;
import ru.korevg.fimas.exception.EntityExistsException;
import ru.korevg.fimas.exception.EntityNotFoundException;
import ru.korevg.fimas.mapper.FirewallMapper;
import ru.korevg.fimas.repository.FirewallRepository;
import ru.korevg.fimas.repository.ModelRepository;
import ru.korevg.fimas.service.impl.FirewallServiceImpl;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirewallServiceImplTest {

    @Mock
    private FirewallRepository firewallRepository;

    @Mock
    private ModelRepository modelRepository;

    @Mock
    private FirewallMapper firewallMapper;

    @InjectMocks
    private FirewallServiceImpl firewallService;

    @Captor
    private ArgumentCaptor<Firewall> firewallCaptor;

    private Model model;
    private Firewall firewall;
    private FirewallResponse expectedResponse;

    @BeforeEach
    void setUp() {
        Vendor vendor = new Vendor();
        vendor.setName("Palo Alto Networks");

        model = new Model();
        model.setId(10L);
        model.setName("PA-220");
        model.setVendor(vendor);

        firewall = Firewall.builder()
                .id(42L)
                .name("edge-fw-01")
                .description("Main office firewall")
                .model(model)
                .policies(Set.of())
                .dynamicAddresses(Set.of())
                .build();

        expectedResponse = new FirewallResponse(
                42L,
                "edge-fw-01",
                "Main office firewall",
                "PA-220",
                "Palo Alto Networks"
        );
    }

    // ──────────────────────────────────────────────────────────────
    //  create
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create: успешное создание → модель привязана, сохранено, ответ маппится")
    void create_success() {
        FirewallCreateRequest request = new FirewallCreateRequest("edge-fw-01", "Main office firewall", 10L);

        when(firewallRepository.existsByName("edge-fw-01")).thenReturn(false);
        when(modelRepository.findById(10L)).thenReturn(Optional.of(model));

        Firewall entityFromMapper = new Firewall();
        entityFromMapper.setName("edge-fw-01");
        entityFromMapper.setDescription("Main office firewall");

        when(firewallMapper.toEntity(request)).thenReturn(entityFromMapper);
        when(firewallRepository.save(any(Firewall.class))).thenAnswer(inv -> {
            Firewall arg = inv.getArgument(0);
            arg.setId(42L); // симулируем @GeneratedValue
            return arg;
        });
        when(firewallMapper.toResponse(any(Firewall.class))).thenReturn(expectedResponse);

        FirewallResponse result = firewallService.create(request);

        assertThat(result).isEqualTo(expectedResponse);

        verify(firewallRepository).existsByName("edge-fw-01");
        verify(modelRepository).findById(10L);
        verify(firewallMapper).toEntity(request);

        verify(firewallRepository).save(firewallCaptor.capture());
        Firewall saved = firewallCaptor.getValue();
        assertThat(saved.getModel()).isSameAs(model);
        assertThat(saved.getName()).isEqualTo("edge-fw-01");
        assertThat(saved.getDescription()).isEqualTo("Main office firewall");

        verify(firewallMapper).toResponse(saved);
    }

    @Test
    @DisplayName("create: имя уже существует → EntityExistsException")
    void create_nameExists_throws() {
        FirewallCreateRequest request = new FirewallCreateRequest("duplicate", null, 10L);

        when(firewallRepository.existsByName("duplicate")).thenReturn(true);

        assertThatThrownBy(() -> firewallService.create(request))
                .isInstanceOf(EntityExistsException.class)
                .hasMessageContaining("Firewall с именем 'duplicate' уже существует");

        verifyNoMoreInteractions(modelRepository, firewallMapper, firewallRepository);
    }

    @Test
    @DisplayName("create: модель не найдена → EntityNotFoundException")
    void create_modelNotFound_throws() {
        FirewallCreateRequest request = new FirewallCreateRequest("new-fw", null, 999L);

        when(firewallRepository.existsByName(any())).thenReturn(false);
        when(modelRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> firewallService.create(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Модель с ID 999");

        verifyNoMoreInteractions(firewallMapper, firewallRepository);
    }

    // ──────────────────────────────────────────────────────────────
    //  update
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update: обновление без смены модели")
    void update_noModelChange() {
        FirewallUpdateRequest request = new FirewallUpdateRequest("edge-fw-02", "New description", null);

        when(firewallRepository.findById(42L)).thenReturn(Optional.of(firewall));
        doNothing().when(firewallMapper).updateFromRequest(request, firewall);
        when(firewallRepository.save(firewall)).thenReturn(firewall);
        when(firewallMapper.toResponse(firewall)).thenReturn(expectedResponse);

        FirewallResponse result = firewallService.update(42L, request);

        assertThat(result).isEqualTo(expectedResponse);

        verify(firewallMapper).updateFromRequest(request, firewall);
        verifyNoInteractions(modelRepository);
        verify(firewallRepository).save(firewall);
    }

    @Test
    @DisplayName("update: смена модели")
    void update_withModelChange() {
        FirewallUpdateRequest request = new FirewallUpdateRequest(null, null, 15L);

        Model newModel = new Model();
        newModel.setId(15L);
        newModel.setName("PA-440");
        Vendor v = new Vendor();
        v.setName("Palo Alto Networks");
        newModel.setVendor(v);

        when(firewallRepository.findById(42L)).thenReturn(Optional.of(firewall));
        doNothing().when(firewallMapper).updateFromRequest(request, firewall);
        when(modelRepository.findById(15L)).thenReturn(Optional.of(newModel));
        when(firewallRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(firewallMapper.toResponse(any())).thenReturn(expectedResponse);

        firewallService.update(42L, request);

        verify(modelRepository).findById(15L);
        verify(firewallRepository).save(firewallCaptor.capture());
        assertThat(firewallCaptor.getValue().getModel()).isSameAs(newModel);
    }

    @Test
    @DisplayName("update: firewall не найден → исключение")
    void update_firewallNotFound() {
        when(firewallRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> firewallService.update(999L, new FirewallUpdateRequest("QFX5110", "Juniper L3", 1L)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ──────────────────────────────────────────────────────────────
    //  delete / findById / findAll — оставил без изменений, они уже корректны
    // ──────────────────────────────────────────────────────────────

    // ... (можно добавить/оставить тесты из предыдущей версии)
}
