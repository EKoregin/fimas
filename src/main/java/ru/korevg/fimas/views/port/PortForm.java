package ru.korevg.fimas.views.port;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import ru.korevg.fimas.dto.port.PortCreateRequest;
import ru.korevg.fimas.dto.port.PortResponse;
import ru.korevg.fimas.dto.port.PortUpdateRequest;
import ru.korevg.fimas.entity.Protocol;
import ru.korevg.fimas.service.PortService;
import ru.korevg.fimas.validation.PortInputUtil;

import java.util.function.Consumer;
import java.util.stream.Collectors;

public class PortForm extends VerticalLayout {

    private final PortService portService;

    // Поля формы
    private final ComboBox<Protocol> protocolField = new ComboBox<>("Протокол");
    private final TextField srcPortField = new TextField("Исходный порт (от)");
    private final TextField dstPortField = new TextField("Порт назначения (до)");

    private BeanValidationBinder<PortCreateRequest> createBinder;
    private BeanValidationBinder<PortUpdateRequest> updateBinder;

    private PortResponse currentPort;
    private Dialog dialog;

    // Коллбэки
    public Consumer<PortResponse> onPortCreatedOrUpdated;
    public Runnable onCancel;

    public PortForm(PortService portService) {
        this.portService = portService;

        setPadding(true);
        setSpacing(true);
        addClassName("port-form");

        configureFields();
        configureValidation();

        protocolField.addValueChangeListener(ev -> {
            boolean icmp = ev.getValue() == Protocol.ICMP;
            srcPortField.setEnabled(!icmp);
            dstPortField.setEnabled(!icmp);

            if (icmp) {
                srcPortField.clear();
                dstPortField.clear();
            }
        });


        add(
                new H3("Порт"),
                protocolField,
                srcPortField,
                dstPortField
        );
    }

    private void configureFields() {
        // Протокол
        protocolField.setItems(Protocol.values());
        protocolField.setItemLabelGenerator(Protocol::name);
        protocolField.setAllowCustomValue(false);
        protocolField.setRequiredIndicatorVisible(true);
        protocolField.setHelperText("Обязательно");

        // Исходный порт
        srcPortField.setHelperText("0–65535 или диапазон 1024–5000 (можно оставить пустым → любой)");
        srcPortField.setAllowedCharPattern("[\\d-]");
        srcPortField.setWidthFull();

        // Порт назначения
        //dstPortField.setRequiredIndicatorVisible(true);
        dstPortField.setHelperText("0–65535 или диапазон 80–443 (можно оставить пустым → любой)");
        dstPortField.setAllowedCharPattern("[\\d-]");
        dstPortField.setWidthFull();
    }

    private void configureValidation() {
        // Binder для создания (PortCreateRequest)
        createBinder = new BeanValidationBinder<>(PortCreateRequest.class);

        createBinder.forField(protocolField)
                .asRequired("Протокол обязателен")
                .withConverter(Protocol::name, Protocol::valueOf, "Неверный протокол")
                .bind(PortCreateRequest::protocol, null);

        createBinder.forField(srcPortField)
                .withNullRepresentation("")
                .withValidator(PortInputUtil::isValidPortInput, "Неверный формат порта или диапазона")
                .bind(PortCreateRequest::srcPort, null);

        createBinder.forField(dstPortField)
                //.asRequired("Порт назначения обязателен")
                .withNullRepresentation("")
                .withValidator(PortInputUtil::isValidPortInput, "Неверный формат порта или диапазона")
                .bind(PortCreateRequest::dstPort, null);

        // Binder для обновления (PortUpdateRequest) — почти такой же, но без обязательности
        updateBinder = new BeanValidationBinder<>(PortUpdateRequest.class);

        updateBinder.forField(protocolField)
                .withConverter(Protocol::name, Protocol::valueOf, "Неверный протокол")
                .bind(PortUpdateRequest::protocol, null);

        updateBinder.forField(srcPortField)
                .withNullRepresentation("")
                .withValidator(PortInputUtil::isValidPortInput, "Неверный формат порта или диапазона")
                .bind(PortUpdateRequest::srcPort, null);

        updateBinder.forField(dstPortField)
                .withNullRepresentation("")
                .withValidator(PortInputUtil::isValidPortInput, "Неверный формат порта или диапазона")
                .bind(PortUpdateRequest::dstPort, null);
    }

    public void openCreateDialog(Consumer<PortResponse> onSuccess, Runnable onCancel) {
        this.currentPort = null;
        this.onPortCreatedOrUpdated = onSuccess;
        this.onCancel = onCancel;

        clearFields();
        dialog = createDialog("Создание нового порта", true);
        dialog.open();
    }

    public void openEditDialog(PortResponse port, Consumer<PortResponse> onSuccess, Runnable onCancel) {
        this.currentPort = port;
        this.onPortCreatedOrUpdated = onSuccess;
        this.onCancel = onCancel;

        fillFieldsFromPort(port);
        dialog = createDialog("Редактирование порта #" + port.id(), false);
        dialog.open();
    }

    // Для использования как субформы внутри ServiceForm (без собственного Dialog)
    public void initAsSubForm() {
        this.currentPort = null;   // всегда создание
        clearFields();
        // dialog не создаём — форму уже добавили в layout
    }

    public void setValuesForCreate(PortCreateRequest initial) {
        if (initial != null) {
            protocolField.setValue(Protocol.valueOf(initial.protocol()));
            srcPortField.setValue(initial.srcPort() != null ? initial.srcPort() : "");
            dstPortField.setValue(initial.dstPort());
        }
    }

    private Dialog createDialog(String title, boolean isCreateMode) {
        Dialog d = new Dialog();
        d.setHeaderTitle(title);
        d.setWidth("540px");
        d.setResizable(true);

        Button actionButton = new Button(isCreateMode ? "Создать" : "Сохранить");
        actionButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        actionButton.addClickListener(e -> save());

        Button cancelBtn = new Button("Отмена", e -> {
            d.close();
            if (onCancel != null) onCancel.run();
        });
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout buttons = new HorizontalLayout(actionButton, cancelBtn);
        buttons.setJustifyContentMode(JustifyContentMode.END);
        buttons.setWidthFull();

        VerticalLayout content = new VerticalLayout(this, buttons);
        content.setPadding(false);
        content.setSpacing(false);

        d.add(content);
        return d;
    }

    public void save() {
        BeanValidationBinder<?> activeBinder = (currentPort == null) ? createBinder : updateBinder;

        BinderValidationStatus<?> status = activeBinder.validate();
        if (!status.isOk()) {
            showValidationErrors(status);
            return;
        }

        try {
            PortResponse result;

            if (currentPort == null) {
                // Создание
                PortCreateRequest req = new PortCreateRequest(
                        protocolField.getValue().name(),
                        srcPortField.isEmpty() ? null : srcPortField.getValue().trim(),
                        dstPortField.getValue().trim()
                );
                result = portService.create(req);
            } else {
                // Обновление
                PortUpdateRequest req = new PortUpdateRequest(
                        protocolField.isEmpty() ? null : protocolField.getValue().name(),
                        srcPortField.isEmpty() ? null : srcPortField.getValue().trim(),
                        dstPortField.isEmpty() ? null : dstPortField.getValue().trim()
                );
                result = portService.update(currentPort.id(), req);
            }

            PortResponse shortResp = new PortResponse(
                    result.id(),
                    result.protocol(),
                    result.srcPort(),
                    result.dstPort()
            );

            Notification.show(
                    currentPort == null ? "Порт создан" : "Порт обновлён",
                    3000, Notification.Position.TOP_CENTER
            ).addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            if (onPortCreatedOrUpdated != null) {
                onPortCreatedOrUpdated.accept(shortResp);
            }

            if (dialog != null) {
                dialog.close();
            }

        } catch (Exception ex) {
            Notification.show("Ошибка: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void showValidationErrors(BinderValidationStatus<?> status) {
        String msg = status.getFieldValidationErrors().stream()
                .map(e -> e.getMessage().orElse("Ошибка"))
                .collect(Collectors.joining("\n• ", "• ", ""));

        Notification.show("Исправьте ошибки:\n" + msg, 6000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void clearFields() {
        protocolField.clear();
        srcPortField.clear();
        dstPortField.clear();
    }

    private void fillFieldsFromPort(PortResponse p) {
        protocolField.setValue(p.protocol());
        srcPortField.setValue(p.srcPort() != null ? p.srcPort() : "");
        dstPortField.setValue(p.dstPort() != null ? p.dstPort() : "");

        boolean icmp = p.protocol() == Protocol.ICMP;
        srcPortField.setEnabled(!icmp);
        dstPortField.setEnabled(!icmp);
    }

    // Геттеры, если понадобится доступ извне (например, для тестов)
    public ComboBox<Protocol> getProtocolField() {
        return protocolField;
    }

    public TextField getSrcPortField() {
        return srcPortField;
    }

    public TextField getDstPortField() {
        return dstPortField;
    }
}