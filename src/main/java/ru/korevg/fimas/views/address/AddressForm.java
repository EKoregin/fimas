package ru.korevg.fimas.views.address;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import ru.korevg.fimas.dto.address.AddressCommonCreateRequest;
import ru.korevg.fimas.dto.address.AddressDynamicCreateRequest;
import ru.korevg.fimas.dto.address.AddressResponse;
import ru.korevg.fimas.dto.firewall.FirewallResponse;   // ← ваш DTO
import ru.korevg.fimas.service.AddressService;
import ru.korevg.fimas.service.FirewallService;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AddressForm {

    private static final String DISPLAY_STATIC = "STATIC";

    private final AddressService addressService;
    private final FirewallService firewallService;

    private final Dialog dialog = new Dialog();
    private final FormLayout form = new FormLayout();

    private final TextField name = new TextField("Имя");
    private final TextArea description = new TextArea("Описание");
    private final TextArea addressesText = new TextArea("Адреса (по одному на строку)");
    private final RadioButtonGroup<String> type = new RadioButtonGroup<>("Тип адреса");
    private final com.vaadin.flow.component.combobox.ComboBox<FirewallResponse> firewallCombo =
            new com.vaadin.flow.component.combobox.ComboBox<>("Firewall");

    private AddressResponse editing = null;
    private Runnable afterSaveCallback;

    public AddressForm(AddressService addressService, FirewallService firewallService) {
        this.addressService = addressService;
        this.firewallService = firewallService;

        dialog.setWidth("620px");
        dialog.setResizable(true);
        dialog.setDraggable(true);

        configureForm();
    }

    private void configureForm() {
        name.setRequired(true);
        name.setWidthFull();

        description.setWidthFull();

        addressesText.setWidthFull();
        addressesText.setHeight("140px");
        addressesText.setPlaceholder("192.168.1.10\n10.0.0.0/24\n...");

        type.setItems("COMMON", "DYNAMIC");
        type.setItemLabelGenerator(value -> "COMMON".equals(value) ? DISPLAY_STATIC : value);
        type.setValue("COMMON");
        type.addValueChangeListener(e -> {
            boolean dyn = "DYNAMIC".equals(e.getValue());
            firewallCombo.setVisible(dyn);
            firewallCombo.setRequiredIndicatorVisible(dyn);
        });

        firewallCombo.setItems(firewallService.findAll());
        firewallCombo.setItemLabelGenerator(FirewallResponse::name);
        firewallCombo.setWidthFull();
        firewallCombo.setVisible(false);

        form.add(name, description, addressesText, type, firewallCombo);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button save = new Button("Создать", e -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Отмена", e -> dialog.close());

        HorizontalLayout buttons = new HorizontalLayout(save, cancel);
        buttons.setWidthFull();
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        VerticalLayout content = new VerticalLayout(form, buttons);
        dialog.add(content);
    }

    public void setAfterSaveCallback(Runnable callback) {
        this.afterSaveCallback = callback;
    }

    public void openCreate() {
        editing = null;
        clear();
        type.setReadOnly(false);
        dialog.setHeaderTitle("Новый адрес");
        ((Button) ((HorizontalLayout) ((VerticalLayout) dialog.getChildren().findFirst().get())
                .getComponentAt(1)).getComponentAt(0)).setText("Создать");
        dialog.open();
    }

    public void openEdit(AddressResponse response) {
        editing = response;
        clear();
        fill(response);

        type.setValue(response.addressType());
        type.setReadOnly(true);                     // тип менять нельзя при редактировании

        dialog.setHeaderTitle("Редактирование адреса");
        ((Button) ((HorizontalLayout) ((VerticalLayout) dialog.getChildren().findFirst().get())
                .getComponentAt(1)).getComponentAt(0)).setText("Сохранить");
        dialog.open();
    }

    private void clear() {
        name.clear();
        description.clear();
        addressesText.clear();
        firewallCombo.clear();
        type.setValue("COMMON");
    }

    private void fill(AddressResponse r) {
        name.setValue(r.name());
        description.setValue(r.description() != null ? r.description() : "");
        addressesText.setValue(r.addresses() != null ? String.join("\n", r.addresses()) : "");

        if ("DYNAMIC".equals(r.addressType()) && r.firewallId() != null) {
            firewallCombo.getListDataView().getItems()
                    .filter(fw -> fw.id().equals(r.firewallId()))
                    .findFirst()
                    .ifPresent(firewallCombo::setValue);
        }
    }

    private void save() {
        if (name.isEmpty()) {
            name.setInvalid(true);
            Notification.show("Имя обязательно", 3000, Notification.Position.MIDDLE);
            return;
        }

        Set<String> addrSet = Arrays.stream(addressesText.getValue().split("\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        try {
            if (editing == null) { // CREATE
                String t = type.getValue();
                if ("COMMON".equals(t)) {
                    addressService.createCommon(new AddressCommonCreateRequest(
                            name.getValue(), description.getValue(), addrSet));
                } else {
                    Long fwId = firewallCombo.getValue() != null ? firewallCombo.getValue().id() : null;
                    addressService.createDynamic(new AddressDynamicCreateRequest(
                            name.getValue(), description.getValue(), addrSet, fwId));
                }
            } else { // UPDATE
                String t = editing.addressType();
                if ("COMMON".equals(t)) {
                    addressService.updateCommon(editing.id(), new AddressCommonCreateRequest(
                            name.getValue(), description.getValue(), addrSet));
                } else {
                    Long fwId = firewallCombo.getValue() != null
                            ? firewallCombo.getValue().id()
                            : editing.firewallId();

                    addressService.updateDynamic(editing.id(), new AddressDynamicCreateRequest(
                            name.getValue(), description.getValue(), addrSet, fwId));
                }
            }

            dialog.close();
            Notification.show("Адрес сохранён", 3000, Notification.Position.TOP_CENTER);

            if (afterSaveCallback != null) {
                afterSaveCallback.run();
            }
        } catch (Exception e) {
            Notification.show("Ошибка: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }
}