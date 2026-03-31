package ru.korevg.fimas.views.policy;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.Query;
import ru.korevg.fimas.dto.address.AddressShortResponse;
import ru.korevg.fimas.dto.policy.PolicyCreateRequest;
import ru.korevg.fimas.dto.policy.PolicyResponse;
import ru.korevg.fimas.dto.policy.PolicyUpdateRequest;
import ru.korevg.fimas.dto.service.ServiceShortResponse;
import ru.korevg.fimas.dto.zone.ZoneResponse;
import ru.korevg.fimas.entity.PolicyAction;
import ru.korevg.fimas.entity.PolicyStatus;
import ru.korevg.fimas.service.AddressService;
import ru.korevg.fimas.service.PolicyService;
import ru.korevg.fimas.service.ServiceService;
import ru.korevg.fimas.service.ZoneService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PolicyForm extends Dialog {

    private final PolicyService policyService;
    private final Long firewallId;
    private final AddressService addressService;
    private final ZoneService zoneService;

    private final ComboBox<Integer> orderCombo = new ComboBox<>("Порядковый номер");
    private final List<Integer> policyOrders;
    private final TextField name = new TextField("Имя политики");
    private final ComboBox<ZoneResponse> srcZoneCombo = new ComboBox<>("Source Zone");
    private final ComboBox<ZoneResponse> dstZoneCombo = new ComboBox<>("Destination Zone");

    private final TextArea description = new TextArea("Описание");
    private final ComboBox<PolicyAction> actionCombo = new ComboBox<>("Действие");
    private final ComboBox<PolicyStatus> statusCombo = new ComboBox<>("Статус");
    private final Checkbox isLogging = new Checkbox("LOG");
    private final Checkbox isNat = new Checkbox("NAT");

    private final MultiSelectComboBox<AddressShortResponse> srcCombo = new MultiSelectComboBox<>("Source Addresses");
    private final MultiSelectComboBox<AddressShortResponse> dstCombo = new MultiSelectComboBox<>("Destination Addresses");
    private final MultiSelectComboBox<ServiceShortResponse> svcCombo = new MultiSelectComboBox<>("Services");

    private PolicyResponse current = null;
    private Runnable afterSave;

    public PolicyForm(PolicyService policyService,
                      Long firewallId,
                      AddressService addressService,
                      ServiceService serviceService,
                      ZoneService zoneService,
                      List<Integer> policyOrders) {
        this.policyService = policyService;
        this.firewallId = firewallId;
        this.addressService = addressService;
        this.zoneService = zoneService;
        this.policyOrders = policyOrders != null
                ? new ArrayList<>(policyOrders)
                : new ArrayList<>();

        configureOrderCombo();
        configureOrderCombo();
        configureZoneCombos();

        setHeaderTitle("Политика");
        setWidth("1000px");
        setResizable(true);

        configureComponents(addressService, serviceService);
        add(createContent());
    }

    private void configureOrderCombo() {
        orderCombo.setLabel("Порядковый номер (не обязательно)");
        orderCombo.setPlaceholder("Выберите или введите номер");
        orderCombo.setWidthFull();
        orderCombo.setAllowCustomValue(true);        // важно — разрешить ввод нового номера
        orderCombo.setItems(policyOrders);           // ← заполняем список

        // Обработчик, если пользователь ввёл своё значение (custom value)
        orderCombo.addCustomValueSetListener(event -> {
            try {
                Integer customValue = Integer.parseInt(event.getDetail());
                // Можно добавить валидацию здесь, если нужно
                orderCombo.setValue(customValue);
            } catch (NumberFormatException e) {
                Notification.show("Порядковый номер должен быть целым числом",
                        3000, Notification.Position.MIDDLE);
                orderCombo.setValue(null);
            }
        });

        orderCombo.setRequired(false);
        orderCombo.setErrorMessage("Выбрать порядковый номер");
    }

    private void configureZoneCombos() {
        srcZoneCombo.setWidthFull();
        dstZoneCombo.setWidthFull();
        srcZoneCombo.setItemLabelGenerator(ZoneResponse::name);
        dstZoneCombo.setItemLabelGenerator(ZoneResponse::name);

        List<ZoneResponse> zones = zoneService.findAll();
        srcZoneCombo.setItems(zones);
        dstZoneCombo.setItems(zones);

        ZoneResponse anyZone = zones.stream()
                .filter(z -> "ANY".equals(z.name()))
                .findFirst()
                .orElse(null);

        srcZoneCombo.setValue(anyZone);
        dstZoneCombo.setValue(anyZone);

        srcZoneCombo.setRequired(false);
        dstZoneCombo.setRequired(false);
    }

    private void configureComponents(AddressService addressService, ServiceService serviceService) {

        name.setRequired(true);
        name.setWidthFull();

        description.setWidthFull();
        description.setMaxLength(500);

        actionCombo.setItems(PolicyAction.values());
        actionCombo.setItemLabelGenerator(Enum::name);
        actionCombo.setRequired(true);

        statusCombo.setItems(PolicyStatus.values());
        statusCombo.setItemLabelGenerator(Enum::name);
        statusCombo.setRequired(true);

        srcCombo.setWidthFull();
        dstCombo.setWidthFull();
        svcCombo.setWidthFull();

        // Загружаем списки
        srcCombo.setItems(addressService.findAllShort(firewallId));
        srcCombo.setItemLabelGenerator(AddressShortResponse::name);

        dstCombo.setItems(addressService.findAllShort(firewallId));
        dstCombo.setItemLabelGenerator(AddressShortResponse::name);

        svcCombo.setItems(serviceService.findAllShort());
        svcCombo.setItemLabelGenerator(ServiceShortResponse::name);
    }

    private VerticalLayout createContent() {
        FormLayout main = new FormLayout(
                orderCombo, name, description,
                actionCombo, statusCombo,
                srcZoneCombo, dstZoneCombo,
                isNat, isLogging
        );

        main.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 1)
        );

        TabSheet tabs = new TabSheet();
        tabs.add("Source Addresses", srcCombo);
        tabs.add("Dest Addresses", dstCombo);
        tabs.add("Сервисы", svcCombo);

        tabs.setWidthFull();
        tabs.setHeightFull();

        Button save = new Button("Сохранить", e -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Отмена", e -> close());

        HorizontalLayout buttons = new HorizontalLayout(save, cancel);
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        VerticalLayout layout = new VerticalLayout(main, tabs, buttons);
        layout.setPadding(true);
        layout.setSpacing(true);

        return layout;
    }

    public void create() {
        current = null;
        clear();
        open();
    }

    public void edit(PolicyResponse policy) {
        current = policy;
        fill(policy);
        open();
    }

    private void clear() {
        name.clear();
        description.clear();
        actionCombo.setValue(PolicyAction.PERMIT);
        statusCombo.setValue(PolicyStatus.ENABLED);
        srcCombo.clear();
        dstCombo.clear();
        svcCombo.clear();

        ZoneResponse anyZone = srcZoneCombo.getDataProvider().fetch(new Query<>())
                .filter(z -> "ANY".equals(z.name()))
                .findFirst()
                .orElse(null);
        srcZoneCombo.setValue(anyZone);
        dstZoneCombo.setValue(anyZone);

        isLogging.clear();
        isNat.clear();
    }

    private void fill(PolicyResponse p) {
        orderCombo.setValue(p.policyOrder());
        name.setValue(p.name());
        description.setValue(p.description() != null ? p.description() : "");
        actionCombo.setValue(p.action());
        statusCombo.setValue(p.status());

        srcCombo.setValue(p.srcAddresses());
        dstCombo.setValue(p.dstAddresses());
        svcCombo.setValue(p.services());

        srcZoneCombo.setValue(p.srcZone());
        dstZoneCombo.setValue(p.dstZone());
        isLogging.setValue(p.isLogging());
        isNat.setValue(p.isNat());
    }

    private void save() {
        if (name.isEmpty() || actionCombo.isEmpty() || statusCombo.isEmpty()) {
            Notification.show("Заполните обязательные поля", 3000, Notification.Position.MIDDLE);
            return;
        }

        try {
            Set<Long> srcIds = srcCombo.getSelectedItems().stream()
                    .map(AddressShortResponse::id)
                    .collect(Collectors.toSet());

            Set<Long> dstIds = dstCombo.getSelectedItems().stream()
                    .map(AddressShortResponse::id)
                    .collect(Collectors.toSet());

            Set<Long> svcIds = svcCombo.getSelectedItems().stream()
                    .map(ServiceShortResponse::id)
                    .collect(Collectors.toSet());

            if (current == null) {
                PolicyCreateRequest req = new PolicyCreateRequest(
                        name.getValue(),
                        description.getValue(),
                        actionCombo.getValue(),
                        statusCombo.getValue(),
                        firewallId,
                        srcIds,
                        dstIds,
                        svcIds,
                        null,
                        srcZoneCombo.getValue() != null ? srcZoneCombo.getValue().id() : null,
                        dstZoneCombo.getValue() != null ? dstZoneCombo.getValue().id() : null,
                        Boolean.FALSE,
                        Boolean.FALSE
                );
                policyService.create(req);
            } else {
                PolicyUpdateRequest req = new PolicyUpdateRequest(
                        name.getValue(),
                        description.getValue(),
                        actionCombo.getValue(),
                        statusCombo.getValue(),
                        firewallId,
                        srcIds,
                        dstIds,
                        svcIds,
                        orderCombo.getValue(),
                        srcZoneCombo.getValue() != null ? srcZoneCombo.getValue().id() : null,
                        dstZoneCombo.getValue() != null ? dstZoneCombo.getValue().id() : null,
                        isLogging.getValue(),
                        isNat.getValue()
                );
                policyService.update(current.id(), req);
            }

            Notification.show("Политика сохранена", 3000, Notification.Position.TOP_CENTER);
            if (afterSave != null)
                afterSave.run();
            close();

        } catch (Exception e) {
            Notification.show("Ошибка: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    public void setAfterSave(Runnable callback) {
        this.afterSave = callback;
    }
}
