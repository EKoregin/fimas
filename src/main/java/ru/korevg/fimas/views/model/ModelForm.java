package ru.korevg.fimas.views.model;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import lombok.Setter;
import ru.korevg.fimas.dto.model.ModelCreateRequest;
import ru.korevg.fimas.dto.model.ModelResponse;
import ru.korevg.fimas.dto.model.ModelUpdateRequest;
import ru.korevg.fimas.dto.vendor.VendorResponse;
import ru.korevg.fimas.service.ModelService;
import ru.korevg.fimas.service.VendorService;

import java.util.function.Consumer;

public class ModelForm extends Dialog {

    private static final String TITLE_NEW = "Новая модель";
    private static final String TITLE_EDIT = "Редактирование модели: %s";
    private static final String SUCCESS_CREATE = "Модель создана";
    private static final String SUCCESS_UPDATE = "Модель обновлена";
    private static final String ERROR_VALIDATION = "Проверьте обязательные поля";
    private static final String ERROR_SAVE = "Ошибка сохранения: %s";

    private final ModelService modelService;
    private final VendorService vendorService;

    private final Binder<ModelCreateRequest> createBinder = new Binder<>(ModelCreateRequest.class);
    private final Binder<ModelUpdateRequest> updateBinder = new Binder<>(ModelUpdateRequest.class);

    private final TextField nameField = new TextField("Название модели");
    private final ComboBox<VendorResponse> vendorCombo = new ComboBox<>("Производитель");

    private Long editingId;
    @Setter
    private Consumer<Void> afterSaveCallback;

    public ModelForm(ModelService modelService, VendorService vendorService) {
        this.modelService = modelService;
        this.vendorService = vendorService;

        configureDialog();
        configureFieldsAndBinders();
        configureLayout();
        configureButtons();
    }

    private void configureDialog() {
        setHeaderTitle(TITLE_NEW);
        setResizable(true);
        setDraggable(true);
        setWidth("500px");
        setMaxWidth("90vw");
    }

    private void configureFieldsAndBinders() {
        nameField.setRequiredIndicatorVisible(true);
        nameField.setWidthFull();

        vendorCombo.setItemLabelGenerator(VendorResponse::name);
        vendorCombo.setRequiredIndicatorVisible(true);
        vendorCombo.setWidthFull();
        vendorCombo.setItems(vendorService.findAll());

        bindCreateFields();
        bindUpdateFields();
    }

    private void bindCreateFields() {
        createBinder.forField(nameField)
                .asRequired("Название обязательно")
                .bind(ModelCreateRequest::getName, ModelCreateRequest::setName);

        createBinder.forField(vendorCombo)
                .asRequired("Производитель обязателен")
                .withConverter(
                        vendor -> vendor != null ? vendor.id() : null,
                        id -> id != null ? vendorService.findByIdOptional(id).orElse(null) : null
                )
                .bind(ModelCreateRequest::getVendorId, ModelCreateRequest::setVendorId);
    }

    private void bindUpdateFields() {
        updateBinder.forField(nameField)
                .bind(ModelUpdateRequest::getName, ModelUpdateRequest::setName);

        updateBinder.forField(vendorCombo)
                .withConverter(
                        vendor -> vendor != null ? vendor.id() : null,
                        id -> id != null ? vendorService.findByIdOptional(id).orElse(null) : null
                )
                .bind(ModelUpdateRequest::getVendorId, ModelUpdateRequest::setVendorId);
    }

    private void configureLayout() {
        FormLayout form = new FormLayout(nameField, vendorCombo);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        form.setWidthFull();
        add(form);
    }

    private void configureButtons() {
        Button save = new Button("Сохранить", e -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Отмена", e -> close());

        HorizontalLayout buttons = new HorizontalLayout(save, cancel);
        buttons.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);
        buttons.setWidthFull();

        getFooter().add(buttons);
    }

    public void openCreate() {
        editingId = null;
        setHeaderTitle(TITLE_NEW);
        createBinder.readBean(new ModelCreateRequest());
        open();
    }

    public void openEdit(ModelResponse response) {
        editingId = response.id();
        setHeaderTitle(String.format(TITLE_EDIT, response.name()));

        ModelUpdateRequest update = new ModelUpdateRequest();
        update.setName(response.name());
        updateBinder.readBean(update);

        vendorService.findByIdOptional(response.vendorId())
                .ifPresent(vendorCombo::setValue);

        open();
    }

    private void save() {
        try {
            if (editingId == null) {
                saveCreate();
            } else {
                saveUpdate();
            }

            close();
            if (afterSaveCallback != null) {
                afterSaveCallback.accept(null);
            }
        } catch (Exception e) {
            showError(String.format(ERROR_SAVE, e.getMessage()));
        }
    }

    private void saveCreate() {
        ModelCreateRequest request = new ModelCreateRequest();
        if (!createBinder.writeBeanIfValid(request)) {
            showValidationError();
            return;
        }
        modelService.create(request);
        showSuccess(SUCCESS_CREATE);
    }

    private void saveUpdate() {
        ModelUpdateRequest request = new ModelUpdateRequest();
        if (!updateBinder.writeBeanIfValid(request)) {
            showValidationError();
            return;
        }
        modelService.update(editingId, request);
        showSuccess(SUCCESS_UPDATE);
    }

    private void showSuccess(String message) {
        Notification.show(message, 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showValidationError() {
        Notification.show(ERROR_VALIDATION, 4000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void showError(String message) {
        Notification.show(message, 5000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

}














//package ru.korevg.fimas.views.model;
//
//import com.vaadin.flow.component.button.Button;
//import com.vaadin.flow.component.button.ButtonVariant;
//import com.vaadin.flow.component.combobox.ComboBox;
//import com.vaadin.flow.component.dialog.Dialog;
//import com.vaadin.flow.component.formlayout.FormLayout;
//import com.vaadin.flow.component.notification.Notification;
//import com.vaadin.flow.component.notification.NotificationVariant;
//import com.vaadin.flow.component.orderedlayout.FlexComponent;
//import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
//import com.vaadin.flow.component.textfield.TextField;
//import com.vaadin.flow.data.binder.Binder;
//import ru.korevg.fimas.dto.model.ModelCreateRequest;
//import ru.korevg.fimas.dto.model.ModelResponse;
//import ru.korevg.fimas.dto.model.ModelUpdateRequest;
//import ru.korevg.fimas.dto.vendor.VendorResponse;
//import ru.korevg.fimas.service.ModelService;
//import ru.korevg.fimas.service.VendorService;
//
//import java.util.function.Consumer;
//
///**
// * Форма создания / редактирования модели (Model)
// */
//public class ModelForm extends Dialog {
//
//    private final ModelService modelService;
//    private final VendorService vendorService;  // предполагается, что есть такой сервис
//
//    private final Binder<ModelCreateRequest> createBinder = new Binder<>(ModelCreateRequest.class);
//    private final Binder<ModelUpdateRequest> updateBinder = new Binder<>(ModelUpdateRequest.class);
//
//    private final TextField nameField = new TextField("Название модели");
//    private final ComboBox<VendorResponse> vendorCombo = new ComboBox<>("Производитель");
//
//    private Long editingId = null; // null = создание, не null = редактирование
//    private Consumer<Void> afterSaveCallback;
//
//    public ModelForm(ModelService modelService, VendorService vendorService) {
//        this.modelService = modelService;
//        this.vendorService = vendorService;
//
//        setHeaderTitle("Модель");
//        setResizable(true);
//        setDraggable(true);
//        setWidth("500px");
//        setMaxWidth("90vw");
//
//        configureFieldsAndBinders();
//        configureLayout();
//        configureButtons();
//    }
//
//    private void configureFieldsAndBinders() {
//        nameField.setRequiredIndicatorVisible(true);
//        nameField.setWidthFull();
//
//        vendorCombo.setItemLabelGenerator(VendorResponse::name);
//        vendorCombo.setRequiredIndicatorVisible(true);
//        vendorCombo.setWidthFull();
//
//        // Загружаем всех вендоров один раз
//        vendorCombo.setItems(vendorService.findAll());
//
//        // Привязка полей к createBinder (для создания)
//        createBinder.forField(nameField)
//                .asRequired("Название обязательно")
//                .bind(ModelCreateRequest::getName, ModelCreateRequest::setName);
//
//        createBinder.forField(vendorCombo)
//                .asRequired("Производитель обязателен")
//                .withConverter(
//                        vendor -> vendor != null ? vendor.id() : null,
//                        id -> {
//                            if (id == null) {
//                                return null;
//                            }
//                            return vendorService.findByIdOptional(id).orElse(null);
//                        }
//                )
//                .bind(ModelCreateRequest::getVendorId, ModelCreateRequest::setVendorId);
//
//        // Привязка полей к updateBinder (для редактирования)
//        updateBinder.forField(nameField)
//                .bind(ModelUpdateRequest::getName, ModelUpdateRequest::setName);
//
//        updateBinder.forField(vendorCombo)
//                .withConverter(
//                        vendor -> vendor != null ? vendor.id() : null,
//                        id -> {
//                            if (id == null) {
//                                return null;
//                            }
//                            return vendorService.findByIdOptional(id).orElse(null);
//                        }
//                )
//                .bind(ModelUpdateRequest::getVendorId, ModelUpdateRequest::setVendorId);
//    }
//
//    private void configureLayout() {
//        FormLayout formLayout = new FormLayout(nameField, vendorCombo);
//        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
//        formLayout.setWidthFull();
//
//        add(formLayout);
//    }
//
//    private FormLayout createFormLayout() {
//        FormLayout formLayout = new FormLayout();
//        formLayout.setResponsiveSteps(
//                new FormLayout.ResponsiveStep("0", 1)
//        );
//        formLayout.add(nameField, vendorCombo);
//        return formLayout;
//    }
//
//    private void configureButtons() {
//        Button saveBtn = new Button("Сохранить");
//        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
//        saveBtn.addClickListener(e -> save());
//
//        Button cancelBtn = new Button("Отмена", e -> close());
//
//        HorizontalLayout buttons = new HorizontalLayout(saveBtn, cancelBtn);
//        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
//        buttons.setWidthFull();
//
//        getFooter().add(buttons);
//    }
//
//    // ────────────────────────────────────────────────
//    // Режим создания
//    // ────────────────────────────────────────────────
//    public void openCreate() {
//        this.editingId = null;
//        setHeaderTitle("Новая модель");
//        createBinder.readBean(new ModelCreateRequest());
//        nameField.clear();
//        vendorCombo.clear();
//        open();
//    }
//
//    // ────────────────────────────────────────────────
//    // Режим редактирования
//    // ────────────────────────────────────────────────
//    public void openEdit(ModelResponse response) {
//        this.editingId = response.id();
//        setHeaderTitle("Редактирование модели: " + response.name());
//
//        // Подготавливаем объект для редактирования
//        ModelUpdateRequest update = new ModelUpdateRequest();
//        update.setName(response.name());
//
//        updateBinder.readBean(update);
//        // Устанавливаем текущее значение вендора
//        vendorService.findByIdOptional(response.vendorId())
//                .ifPresent(vendorCombo::setValue);
//
//        nameField.setValue(response.name() != null ? response.name() : "");
//
//        open();
//    }
//
//    private void save() {
//        try {
//            if (editingId == null) {
//                // Создание
//                ModelCreateRequest request = new ModelCreateRequest();
//                if (!createBinder.writeBeanIfValid(request)) {
//                    showValidationError();
//                    return;
//                }
//
//                modelService.create(request);
//                Notification.show("Модель создана")
//                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
//            } else {
//                // Обновление
//                ModelUpdateRequest request = new ModelUpdateRequest();
//                if (!updateBinder.writeBeanIfValid(request)) {
//                    showValidationError();
//                    return;
//                }
//
//                modelService.update(editingId, request);
//                Notification.show("Модель обновлена")
//                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
//            }
//
//            close();
//            if (afterSaveCallback != null) {
//                afterSaveCallback.accept(null);
//            }
//
//        } catch (Exception e) {
//            Notification.show("Ошибка сохранения: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
//                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
//        }
//    }
//
//    private void showValidationError() {
//        Notification.show("Проверьте правильность заполнения полей", 4000, Notification.Position.MIDDLE)
//                .addThemeVariants(NotificationVariant.LUMO_ERROR);
//    }
//
//    public void setAfterSaveCallback(Consumer<Void> callback) {
//        this.afterSaveCallback = callback;
//    }
//
//}
