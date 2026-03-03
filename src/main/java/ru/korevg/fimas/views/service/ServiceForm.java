package ru.korevg.fimas.views.service;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import ru.korevg.fimas.dto.port.PortCreateRequest;
import ru.korevg.fimas.dto.port.PortResponse;
import ru.korevg.fimas.dto.port.PortShortResponse;
import ru.korevg.fimas.dto.service.ServiceCreateRequest;
import ru.korevg.fimas.dto.service.ServiceResponse;
import ru.korevg.fimas.dto.service.ServiceUpdateRequest;
import ru.korevg.fimas.entity.Protocol;
import ru.korevg.fimas.service.PortService;
import ru.korevg.fimas.service.ServiceService;
import ru.korevg.fimas.validation.PortInputUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ServiceForm extends VerticalLayout {

    private final ServiceService serviceService;
    private final PortService portService;

    private final TextField nameField = new TextField("Имя сервиса");
    private final TextArea descriptionField = new TextArea("Описание");

    private final Grid<PortShortResponse> selectedPortsGrid = new Grid<>(PortShortResponse.class, false);
    private final Set<PortShortResponse> selectedPorts = new HashSet<>();

    private ServiceResponse currentService;
    private Dialog dialog;
    private Runnable afterSaveCallback;

    public ServiceForm(ServiceService serviceService, PortService portService) {
        this.serviceService = serviceService;
        this.portService = portService;

        setPadding(true);
        setSpacing(true);

        configureFields();
        configureSelectedPortsGrid();

        add(
                new H3("Сервис"),
                nameField,
                descriptionField,
                new H3("Выбранные порты"),
                selectedPortsGrid,
                createButtonsLayout()
        );
    }

    public void setAfterSaveCallback(Runnable callback) {
        this.afterSaveCallback = callback;
    }

    private void configureFields() {
        nameField.setRequiredIndicatorVisible(true);
        nameField.setWidthFull();

        descriptionField.setWidthFull();
        descriptionField.setMaxLength(500);
    }

    private void configureSelectedPortsGrid() {
        selectedPortsGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES);
        selectedPortsGrid.setHeight("220px");

        selectedPortsGrid.addColumn(PortShortResponse::getProtocol).setHeader("Протокол").setWidth("100px");
        selectedPortsGrid.addColumn(PortShortResponse::getSrcPort).setHeader("Исх. порт").setWidth("110px");
        selectedPortsGrid.addColumn(PortShortResponse::getDstPort).setHeader("Порт назн.").setWidth("110px");

        selectedPortsGrid.addComponentColumn(port -> {
            Button removeBtn = new Button("Удалить");
            removeBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            removeBtn.addClickListener(e -> {
                selectedPorts.remove(port);
                refreshSelectedPortsGrid();
            });
            return removeBtn;
        }).setHeader("Действия").setWidth("100px").setFlexGrow(0);

        Button addPortsBtn = new Button("Добавить порты", e -> openPortSelectionDialog());
        addPortsBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout actions = new HorizontalLayout(addPortsBtn);
        actions.setAlignItems(Alignment.CENTER);

        VerticalLayout portsSection = new VerticalLayout(selectedPortsGrid, actions);
        portsSection.setPadding(false);
        add(portsSection);
    }

    private void openPortSelectionDialog() {
        Dialog portDialog = new Dialog();
        portDialog.setHeaderTitle("Выбор портов");
        portDialog.setWidth("700px");
        portDialog.setHeight("500px");

        Grid<PortShortResponse> allPortsGrid = new Grid<>(PortShortResponse.class, false);
        allPortsGrid.setItems(portService.findAllShort());  // начальная загрузка

        allPortsGrid.addColumn(PortShortResponse::getProtocol).setHeader("Протокол");
        allPortsGrid.addColumn(PortShortResponse::getSrcPort).setHeader("Исх. порт");
        allPortsGrid.addColumn(PortShortResponse::getDstPort).setHeader("Порт назн.");

        allPortsGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        allPortsGrid.asMultiSelect().setValue(selectedPorts);

        // Кнопка создания нового порта
        Button createNewPortBtn = new Button("Создать новый порт", e -> openCreatePortDialog(portDialog, allPortsGrid));
        createNewPortBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

        Button confirmBtn = new Button("Добавить выбранные", e -> {
            selectedPorts.clear();
            selectedPorts.addAll(allPortsGrid.getSelectedItems());
            refreshSelectedPortsGrid();
            portDialog.close();
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Отмена", e -> portDialog.close());

        HorizontalLayout buttons = new HorizontalLayout(createNewPortBtn, confirmBtn, cancelBtn);
        buttons.setJustifyContentMode(JustifyContentMode.BETWEEN);  // распределить по краям
        buttons.setWidthFull();

        VerticalLayout content = new VerticalLayout(allPortsGrid, buttons);
        content.setSizeFull();

        portDialog.add(content);
        portDialog.open();
    }

    private void openCreatePortDialog(Dialog parentDialog, Grid<PortShortResponse> allPortsGrid) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Создание нового порта");
        dialog.setWidth("540px");
        dialog.setResizable(true);

        BeanValidationBinder<PortCreateRequest> binder = new BeanValidationBinder<>(PortCreateRequest.class);

        // Поля формы
        ComboBox<Protocol> protocolField = createProtocolField();
        TextField srcPortField = createSrcPortField();
        TextField dstPortField = createDstPortField();

        // Привязки к binder
        configureBindings(binder, protocolField, srcPortField, dstPortField);

        VerticalLayout form = new VerticalLayout(protocolField, srcPortField, dstPortField);
        form.setPadding(true);
        form.setSpacing(true);

        Button saveButton = new Button("Создать и добавить", event -> {
            BinderValidationStatus<PortCreateRequest> status = binder.validate();

            if (!status.isOk()) {
                showValidationErrors(status);
                return;
            }

            PortCreateRequest request = buildRequestFromFields(protocolField, srcPortField, dstPortField);

            try {
                PortResponse created = portService.create(request);
                PortShortResponse newItem = mapToShortResponse(created);

                // Добавляем в выбранные
                selectedPorts.add(newItem);

                // Обновляем список в гриде без дубликатов
                updateGridItems(allPortsGrid, newItem);

                // Восстанавливаем / устанавливаем выделение после обновления списка
                allPortsGrid.asMultiSelect().select(selectedPorts);

                Notification.show("Порт успешно создан и добавлен", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                dialog.close();

            } catch (Exception e) {
                Notification.show("Не удалось создать порт: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Отмена", e -> dialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout buttons = new HorizontalLayout(saveButton, cancelButton);
        buttons.setJustifyContentMode(JustifyContentMode.END);
        buttons.setWidthFull();

        VerticalLayout content = new VerticalLayout(form, buttons);
        content.setSizeFull();
        content.setPadding(true);

        dialog.add(content);
        dialog.open();
    }

// ──────────────────────────────────────────────────────────────────────────────
// Вспомогательные методы (для лучшей читаемости и поддержки кода)
// ──────────────────────────────────────────────────────────────────────────────

    private ComboBox<Protocol> createProtocolField() {
        ComboBox<Protocol> field = new ComboBox<>("Протокол");
        field.setItems(Protocol.values());
        field.setItemLabelGenerator(Protocol::name);
        field.setAllowCustomValue(false);
        field.setRequiredIndicatorVisible(true);
        field.setHelperText("Обязательно");
        return field;
    }

    private TextField createSrcPortField() {
        TextField field = new TextField("Исходный порт (от)");
        field.setHelperText("0–65535 или диапазон 1024–5000 (можно оставить пустым → любой)");
        field.setAllowedCharPattern("[\\d-]");
        return field;
    }

    private TextField createDstPortField() {
        TextField field = new TextField("Порт назначения (до)");
        field.setRequiredIndicatorVisible(true);
        field.setHelperText("0–65535 или диапазон 80–443 (обязательно)");
        field.setAllowedCharPattern("[\\d-]");
        return field;
    }

    private void configureBindings(BeanValidationBinder<PortCreateRequest> binder,
                                   ComboBox<Protocol> protocolField,
                                   TextField srcPortField,
                                   TextField dstPortField) {

        binder.forField(protocolField)
                .asRequired("Протокол обязателен")
                .withConverter(Protocol::name, Protocol::valueOf)
                .bind(PortCreateRequest::protocol, null);

        binder.forField(srcPortField)
                .withNullRepresentation("")
                .withValidator(PortInputUtil::isValidPortInput, "Неверный формат порта или диапазона")
                .bind(PortCreateRequest::srcPort, null);

        binder.forField(dstPortField)
                .asRequired("Порт назначения обязателен")
                .withNullRepresentation("")
                .withValidator(PortInputUtil::isValidPortInput, "Неверный формат порта или диапазона")
                .bind(PortCreateRequest::dstPort, null);
    }

    private void showValidationErrors(BinderValidationStatus<PortCreateRequest> status) {
        String message = status.getFieldValidationErrors().stream()
                .map(err -> err.getMessage().orElse("Ошибка"))
                .collect(Collectors.joining("\n• ", "• ", ""));

        Notification.show("Исправьте ошибки в форме:\n" + message, 6000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private PortCreateRequest buildRequestFromFields(ComboBox<Protocol> protocolField,
                                                     TextField srcPortField,
                                                     TextField dstPortField) {
        String protocol = protocolField.getValue().name();
        String src = srcPortField.isEmpty() ? null : srcPortField.getValue().trim();
        String dst = dstPortField.getValue().trim();

        return new PortCreateRequest(protocol, src, dst);
    }

    private PortShortResponse mapToShortResponse(PortResponse response) {
        return new PortShortResponse(
                response.id(),
                response.protocol().name(),
                response.srcPort(),
                response.dstPort()
        );
    }

    private void updateGridItems(Grid<PortShortResponse> grid, PortShortResponse newItem) {
        List<PortShortResponse> items = grid.getListDataView().getItems().toList();

        boolean alreadyExists = items.stream().anyMatch(it -> it.equals(newItem));

        if (alreadyExists) {
            return; // ничего не добавляем
        }

        List<PortShortResponse> updated = new ArrayList<>(items);
        updated.add(newItem);

        grid.setItems(updated);
    }

    private void refreshSelectedPortsGrid() {
        selectedPortsGrid.setItems(selectedPorts);
    }

    private HorizontalLayout createButtonsLayout() {
        Button saveBtn = new Button("Сохранить");
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.addClickListener(e -> saveService());

        Button cancelBtn = new Button("Отмена");
        cancelBtn.addClickListener(e -> {
            if (dialog != null) dialog.close();
        });

        return new HorizontalLayout(saveBtn, cancelBtn);
    }

    private void saveService() {
        // Простая валидация
        if (nameField.isEmpty()) {
            Notification.show("Имя сервиса обязательно")
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        Set<Long> selectedPortIds = selectedPorts.stream()
                .map(PortShortResponse::getId)
                .collect(Collectors.toSet());

        try {
            if (currentService == null) {
                // Создание
                ServiceCreateRequest request = new ServiceCreateRequest(
                        nameField.getValue(),
                        descriptionField.getValue().isBlank() ? null : descriptionField.getValue(),
                        selectedPortIds.isEmpty() ? null : selectedPortIds
                );
                serviceService.create(request);
            } else {
                // Обновление
                ServiceUpdateRequest request = new ServiceUpdateRequest(
                        nameField.getValue().isBlank() ? null : nameField.getValue(),
                        descriptionField.getValue().isBlank() ? null : descriptionField.getValue(),
                        selectedPortIds  // всегда передаём, даже пустой набор — это очистит порты
                );
                serviceService.update(currentService.id(), request);
            }

            Notification.show("Сервис сохранён успешно")
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            if (afterSaveCallback != null) {
                afterSaveCallback.run();
            }

            if (dialog != null) {
                dialog.close();
            }

        } catch (Exception ex) {
            Notification.show("Ошибка сохранения: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    public void openCreateDialog() {
        currentService = null;
        nameField.clear();
        descriptionField.clear();
        selectedPorts.clear();
        refreshSelectedPortsGrid();

        dialog = new Dialog();
        dialog.setHeaderTitle("Создание нового сервиса");
        dialog.add(this);
        dialog.setWidth("900px");
        dialog.setResizable(true);
        dialog.open();
    }

    public void openEditDialog(ServiceResponse service) {
        currentService = service;
        nameField.setValue(service.name() != null ? service.name() : "");
        descriptionField.setValue(service.description() != null ? service.description() : "");
        selectedPorts.clear();
        if (service.ports() != null) {
            selectedPorts.addAll(service.ports());
        }
        refreshSelectedPortsGrid();

        dialog = new Dialog();
        dialog.setHeaderTitle("Редактирование сервиса: " + (service.name() != null ? service.name() : "без имени"));
        dialog.add(this);
        dialog.setWidth("900px");
        dialog.setResizable(true);
        dialog.open();
    }
}