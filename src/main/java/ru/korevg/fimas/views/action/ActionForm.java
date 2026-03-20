package ru.korevg.fimas.views.action;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import lombok.extern.slf4j.Slf4j;
import ru.korevg.fimas.dto.action.ActionCreate;
import ru.korevg.fimas.dto.action.ActionResponse;
import ru.korevg.fimas.dto.action.ActionUpdate;
import ru.korevg.fimas.dto.command.CommandCreate;
import ru.korevg.fimas.dto.command.CommandResponse;
import ru.korevg.fimas.dto.command.CommandUpdate;
import ru.korevg.fimas.entity.CommandType;
import ru.korevg.fimas.entity.Model;
import ru.korevg.fimas.service.ActionCommandService;
import ru.korevg.fimas.service.CommandService;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ActionForm extends Dialog {

    private final ActionCommandService actionService;
    private final CommandService commandService;
    private final Runnable onSaveCallback;
    private final Model currentModel;

    private final TextField nameField = new TextField("Название");
    private final MultiSelectComboBox<CommandResponse> commandsCombo = new MultiSelectComboBox<>("Выбранные команды");
    private final Grid<CommandResponse> commandsGrid = new Grid<>(CommandResponse.class, false);

    private final Binder<ActionCreate> createBinder = new Binder<>(ActionCreate.class);
    private final Binder<ActionUpdate> updateBinder = new Binder<>(ActionUpdate.class);

    private Long editingId;

    public ActionForm(ActionCommandService actionService, CommandService commandService,
                      Runnable onSaveCallback, Model currentModel) {
        this.actionService = actionService;
        this.commandService = commandService;
        this.onSaveCallback = onSaveCallback;
        this.currentModel = currentModel;

        setHeaderTitle("Действие");
        setResizable(true);
        setDraggable(true);
        setWidth("800px");
        setMaxWidth("95vw");

        initFields();
        initBinders();
        initLayout();
        initButtons();
        initCommandsSection();
        reloadCommands();
    }

    private void initFields() {
        nameField.setRequiredIndicatorVisible(true);
        nameField.setWidthFull();

        commandsCombo.setItemLabelGenerator(cmd ->
                cmd.getName() + " (" + cmd.getCommandType() + ")");
        commandsCombo.setWidthFull();
        commandsCombo.setHelperText("Выберите команды или создайте новые");

        reloadCommands();
    }

    private void reloadCommands() {
        Long vendorId = currentModel.getVendor().getId();
        List<CommandResponse> filtered = commandService.getCommandsByVendor(vendorId);
        commandsCombo.setItems(filtered);
        commandsGrid.setItems(filtered);
    }

    private void initBinders() {
        createBinder.forField(nameField)
                .asRequired("Название обязательно")
                .bind(ActionCreate::getName, ActionCreate::setName);

        createBinder.forField(commandsCombo)
                .asRequired("Выберите хотя бы одну команду")
                .withConverter(
                        selected -> selected.stream().map(CommandResponse::getId).collect(Collectors.toSet()),
                        ids -> ids == null || ids.isEmpty()
                                ? Collections.emptySet()
                                : ids.stream()
                                .map(commandService::findById)
                                .filter(java.util.Optional::isPresent)
                                .map(java.util.Optional::get)
                                .collect(Collectors.toSet())
                )
                .bind(ActionCreate::getCommandIds, ActionCreate::setCommandIds);

        updateBinder.forField(nameField)
                .bind(ActionUpdate::getName, ActionUpdate::setName);

        updateBinder.forField(commandsCombo)
                .withConverter(
                        selected -> selected.stream().map(CommandResponse::getId).collect(Collectors.toSet()),
                        ids -> ids == null || ids.isEmpty()
                                ? Collections.emptySet()
                                : ids.stream()
                                .map(commandService::findById)
                                .filter(java.util.Optional::isPresent)
                                .map(java.util.Optional::get)
                                .collect(Collectors.toSet())
                )
                .bind(ActionUpdate::getCommandIds, ActionUpdate::setCommandIds);
    }

    private void initLayout() {
        FormLayout form = new FormLayout(nameField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        form.setWidthFull();

        // Сворачиваемый блок с командами
        VerticalLayout commandsContent = new VerticalLayout(
                commandsGrid
        );
        commandsContent.setPadding(false);
        commandsContent.setSpacing(true);

        Details commandsDetails = new Details("Доступные команды", commandsContent);
        commandsDetails.setOpened(true); // по умолчанию открыт (можно false)

        add(form, commandsDetails, commandsCombo);
    }

    private void initButtons() {
        Button save = new Button("Сохранить", e -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Отмена", e -> close());

        Button addCommand = new Button("Новая команда", VaadinIcon.PLUS.create());
        addCommand.addClickListener(e -> openCommandCreateDialog());

        HorizontalLayout buttons = new HorizontalLayout(addCommand, save, cancel);
        buttons.setJustifyContentMode(HorizontalLayout.JustifyContentMode.BETWEEN);
        buttons.setWidthFull();

        getFooter().add(buttons);
    }

    private void initCommandsSection() {
        // Grid для отображения и управления командами
        commandsGrid.setSizeFull();
        commandsGrid.setHeight("300px");

        commandsGrid.addColumn(CommandResponse::getName).setHeader("Название");
        commandsGrid.addColumn(CommandResponse::getCommand).setHeader("Текст команды");
        commandsGrid.addColumn(cmd -> cmd.getCommandType().name()).setHeader("Тип");

        commandsGrid.addComponentColumn(cmd -> {
            HorizontalLayout actions = new HorizontalLayout();

            Button editBtn = new Button(VaadinIcon.EDIT.create());
            editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            editBtn.setTooltipText("Редактировать команду");
            editBtn.addClickListener(e -> openCommandEditDialog(cmd));

            Button deleteBtn = new Button(VaadinIcon.TRASH.create());
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            deleteBtn.setTooltipText("Удалить команду (если не используется)");
            deleteBtn.addClickListener(e -> confirmDeleteCommand(cmd));

            actions.add(editBtn, deleteBtn);
            return actions;
        }).setHeader("Действия").setAutoWidth(true);
    }

    private void openCommandCreateDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Новая команда");
        dialog.setWidth("min(800px, 92vw)");  // ← увеличил до 900px
        dialog.setMaxWidth("95vw");

        TextField name = new TextField("Название команды");
        name.setRequiredIndicatorVisible(true);
        name.setWidthFull();

        TextArea text = new TextArea("Текст команды");
        text.setRequiredIndicatorVisible(true);
        text.setHeight("120px");
        text.setWidthFull();

        Select<CommandType> type = new Select<>();
        type.setLabel("Тип команды");
        type.setItems(CommandType.values());
        type.setRequiredIndicatorVisible(true);
        type.setPlaceholder("Выберите тип");

        TextField vendorDisplay = new TextField("Вендор");
        vendorDisplay.setValue(currentModel.getVendor().getName());
        vendorDisplay.setReadOnly(true);
        vendorDisplay.setTooltipText("Вендор берётся из модели");

        Binder<CommandCreate> binder = new Binder<>(CommandCreate.class);
        binder.forField(name).asRequired().bind(CommandCreate::getName, CommandCreate::setName);
        binder.forField(text).asRequired().bind(CommandCreate::getCommand, CommandCreate::setCommand);
        binder.forField(type).asRequired().bind(CommandCreate::getCommandType, CommandCreate::setCommandType);

        Button saveBtn = new Button("Создать", e -> {
            CommandCreate req = new CommandCreate();
            if (binder.writeBeanIfValid(req)) {
                try {
                    req.setVendorId(currentModel.getVendor().getId());
                    CommandResponse created = commandService.create(req);
                    reloadCommands();
                    commandsCombo.select(created);
                    dialog.close();
                    Notification.show("Команда создана")
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } catch (Exception ex) {
                    Notification.show("Ошибка: " + ex.getMessage())
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            } else {
                Notification.show("Заполните все поля")
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        Button cancelBtn = new Button("Отмена", e -> dialog.close());

        VerticalLayout layout = new VerticalLayout(name, text, type, vendorDisplay);
        layout.setWidthFull();          // ← растягиваем контейнер
        layout.setPadding(true);
        layout.setSpacing(true);

        dialog.add(layout);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void openCommandEditDialog(CommandResponse cmd) {
        if (commandService.isCommandUsedInAnyAction(cmd.getId())) {
            Notification.show("Команда используется в действиях — редактирование запрещено", 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        Dialog editDialog = new Dialog();
        editDialog.setHeaderTitle("Редактирование команды: " + cmd.getName());
        editDialog.setWidth("min(800px, 92vw)");  // ← увеличил до 900px
        editDialog.setMaxWidth("95vw");

        TextField name = new TextField("Название");
        name.setValue(cmd.getName());
        name.setWidthFull();

        TextArea text = new TextArea("Текст команды");
        text.setValue(cmd.getCommand());
        text.setHeight("120px");
        text.setWidthFull();

        Select<CommandType> type = new Select<>();
        type.setItems(CommandType.values());
        type.setValue(cmd.getCommandType());

        Binder<CommandUpdate> binder = new Binder<>(CommandUpdate.class);
        binder.forField(name).bind(CommandUpdate::getName, CommandUpdate::setName);
        binder.forField(text).bind(CommandUpdate::getCommand, CommandUpdate::setCommand);
        binder.forField(type).bind(CommandUpdate::getCommandType, CommandUpdate::setCommandType);

        Button saveBtn = new Button("Сохранить", e -> {
            CommandUpdate req = new CommandUpdate();
            if (binder.writeBeanIfValid(req)) {
                try {
                    commandService.updateCommand(cmd.getId(), req);
                    reloadCommands();
                    Notification.show("Команда обновлена")
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    editDialog.close();
                } catch (Exception ex) {
                    Notification.show("Ошибка: " + ex.getMessage())
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }
        });

        Button cancelBtn = new Button("Отмена", e -> editDialog.close());


        VerticalLayout layout = new VerticalLayout(name, text, type);
        layout.setWidthFull();
        layout.setPadding(true);
        layout.setSpacing(true);

        editDialog.add(layout);
        editDialog.getFooter().add(cancelBtn, saveBtn);
        editDialog.open();
    }

    private void confirmDeleteCommand(CommandResponse cmd) {
        if (commandService.isCommandUsedInAnyAction(cmd.getId())) {
            Notification.show("Команда используется в действиях — удаление запрещено", 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Удалить команду?");
        confirm.setText("Вы уверены, что хотите удалить команду \"" + cmd.getName() + "\"?");
        confirm.setConfirmText("Удалить");
        confirm.setCancelText("Отмена");

        confirm.addConfirmListener(e -> {
            try {
                commandService.deleteCommand(cmd.getId());
                reloadCommands();
                Notification.show("Команда удалена")
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Ошибка: " + ex.getMessage())
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        confirm.open();
    }

    public void openCreate() {
        editingId = null;
        setHeaderTitle("Новое действие");
        createBinder.readBean(ActionCreate.builder()
                .name("")
                .commandIds(Collections.emptySet())
                .build());
        commandsCombo.deselectAll();
        open();
    }

    public void openEdit(ActionResponse action) {
        editingId = action.getId();
        setHeaderTitle("Редактирование: " + action.getName());

        ActionUpdate update = ActionUpdate.builder()
                .name(action.getName())
                .commandIds(action.getCommands().stream()
                        .map(CommandResponse::getId)
                        .collect(Collectors.toSet()))
                .build();

        updateBinder.readBean(update);
        commandsCombo.setValue(action.getCommands());
        open();
    }

    private void save() {
        try {
            if (editingId == null) {
                ActionCreate req = new ActionCreate();
                req.setModelId(currentModel.getId());
                if (!createBinder.writeBeanIfValid(req)) {
                    showError("Заполните обязательные поля");
                    return;
                }
                actionService.createAction(req);
                showSuccess("Действие создано");
            } else {
                ActionUpdate req = new ActionUpdate();
                if (!updateBinder.writeBeanIfValid(req)) {
                    showError("Заполните обязательные поля");
                    return;
                }
                actionService.updateAction(editingId, req);
                showSuccess("Действие обновлено");
            }

            close();
            if (onSaveCallback != null) onSaveCallback.run();
        } catch (Exception e) {
            showError("Ошибка: " + e.getMessage());
        }
    }

    private void showSuccess(String msg) {
        Notification.show(msg, 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showError(String msg) {
        Notification.show(msg, 5000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
