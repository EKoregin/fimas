package ru.korevg.fimas.views.firewall;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import ru.korevg.fimas.dto.firewall.FirewallCreateRequest;
import ru.korevg.fimas.dto.firewall.FirewallResponse;
import ru.korevg.fimas.dto.firewall.FirewallUpdateRequest;
import ru.korevg.fimas.dto.model.ModelResponse;
import ru.korevg.fimas.service.FirewallService;
import ru.korevg.fimas.service.ModelService;


public class FirewallForm extends VerticalLayout {

    private final FirewallService firewallService;
    private final ModelService modelService;  // добавьте зависимость

    private final Dialog dialog = new Dialog();
    private final TextField nameField = new TextField("Имя");
    private final TextArea descriptionField = new TextArea("Описание");
    private final ComboBox<ModelResponse> modelCombo = new ComboBox<>("Модель");

    private FirewallResponse editing = null;
    private Runnable afterSaveCallback;

    public FirewallForm(FirewallService firewallService, ModelService modelService) {
        this.firewallService = firewallService;
        this.modelService = modelService;

        dialog.setWidth("550px");
        dialog.setResizable(true);

        configureForm();
    }

    private void configureForm() {
        nameField.setRequired(true);
        nameField.setWidthFull();

        descriptionField.setWidthFull();
        descriptionField.setMaxLength(500);

        modelCombo.setItems(modelService.findAll());
        modelCombo.setItemLabelGenerator(ModelResponse::name);
        modelCombo.setRequired(true);
        modelCombo.setWidthFull();

        FormLayout form = new FormLayout(nameField, descriptionField, modelCombo);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button saveBtn = new Button("Сохранить", e -> save());
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Отмена", e -> dialog.close());

        HorizontalLayout buttons = new HorizontalLayout(saveBtn, cancelBtn);
        buttons.setWidthFull();
        buttons.setJustifyContentMode(JustifyContentMode.END);

        VerticalLayout content = new VerticalLayout(form, buttons);
        dialog.add(content);
    }

    public void setAfterSaveCallback(Runnable callback) {
        this.afterSaveCallback = callback;
    }

    public void openCreate() {
        editing = null;
        clearFields();
        dialog.setHeaderTitle("Новый Firewall");
        dialog.open();
    }

    public void openEdit(FirewallResponse response) {
        editing = response;
        fillFields(response);
        dialog.setHeaderTitle("Редактирование Firewall: " + response.name());
        dialog.open();
    }

    private void clearFields() {
        nameField.clear();
        descriptionField.clear();
        modelCombo.clear();
    }

    private void fillFields(FirewallResponse r) {
        nameField.setValue(r.name() != null ? r.name() : "");
        descriptionField.setValue(r.description() != null ? r.description() : "");

        if (r.modelId() != null) {
            modelService.findById(r.modelId())
                    .ifPresent(modelCombo::setValue);  // ← просто setValue, если нашли
        } else {
            modelCombo.clear();
        }
    }

    private void save() {
        if (nameField.isEmpty() || modelCombo.isEmpty()) {
            Notification.show("Имя и модель обязательны", 3000, Notification.Position.MIDDLE);
            return;
        }

        try {
            if (editing == null) {
                // Создание
                FirewallCreateRequest req = new FirewallCreateRequest(
                        nameField.getValue(),
                        descriptionField.getValue(),
                        modelCombo.getValue().id()
                );
                firewallService.create(req);
            } else {
                // Обновление
                FirewallUpdateRequest req = new FirewallUpdateRequest(
                        nameField.getValue(),
                        descriptionField.getValue(),
                        modelCombo.getValue().id()
                );
                firewallService.update(editing.id(), req);
            }

            Notification.show("Firewall сохранён", 3000, Notification.Position.TOP_CENTER);

            if (afterSaveCallback != null) {
                afterSaveCallback.run();
            }

            dialog.close();

        } catch (Exception e) {
            Notification.show("Ошибка: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }
}