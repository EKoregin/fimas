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
import ru.korevg.fimas.config.LocalCommandHandlerRegistry;
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
    private final LocalCommandHandlerRegistry localHandlerRegistry;

    // Поля для Action
    private final TextField nameField = new TextField("Название");
    private final MultiSelectComboBox<CommandResponse> commandsCombo = new MultiSelectComboBox<>("Выбранные команды");
    private final Grid<CommandResponse> commandsGrid = new Grid<>(CommandResponse.class, false);

    // Поля для создания/редактирования Command
    private final TextField commandNameField = new TextField("Название команды");
    private final Select<CommandType> commandTypeSelect = new Select<>();
    private final TextArea commandTextArea = new TextArea("Текст команды");
    private final Select<String> localHandlerSelect = new Select<>();

    private final Binder<ActionCreate> createBinder = new Binder<>(ActionCreate.class);
    private final Binder<ActionUpdate> updateBinder = new Binder<>(ActionUpdate.class);

    private Long editingId; // ID редактируемого Action

    public ActionForm(ActionCommandService actionService,
                      CommandService commandService,
                      Runnable onSaveCallback,
                      Model currentModel,
                      LocalCommandHandlerRegistry localHandlerRegistry) {

        this.actionService = actionService;
        this.commandService = commandService;
        this.onSaveCallback = onSaveCallback;
        this.currentModel = currentModel;
        this.localHandlerRegistry = localHandlerRegistry;

        setHeaderTitle("Действие");
        setResizable(true);
        setDraggable(true);
        setWidth("950px");
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

        // === Поля для команды ===
        commandNameField.setRequiredIndicatorVisible(true);
        commandNameField.setWidthFull();

        commandTypeSelect.setLabel("Тип команды");
        commandTypeSelect.setItems(CommandType.values());
        commandTypeSelect.setRequiredIndicatorVisible(true);
        commandTypeSelect.setPlaceholder("Выберите тип");

        commandTextArea.setRequiredIndicatorVisible(true);
        commandTextArea.setHeight("130px");
        commandTextArea.setWidthFull();

        // Локальный обработчик — только для LOCAL
        localHandlerSelect.setLabel("Локальный обработчик");
        localHandlerSelect.setRequiredIndicatorVisible(true);
        localHandlerSelect.setPlaceholder("Выберите обработчик");
        localHandlerSelect.setVisible(false);

        // Заполняем список обработчиков для текущего вендора
        String vendorKey = currentModel.getVendor().getName().toLowerCase();
        localHandlerSelect.setItems(
                localHandlerRegistry.getHandlersForVendor(vendorKey).keySet()
        );
    }

    private void initBinders() {
        // Binder для Action
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
        FormLayout actionForm = new FormLayout(nameField);
        actionForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        actionForm.setWidthFull();

        // Сворачиваемый блок с доступными командами
        VerticalLayout commandsContent = new VerticalLayout(commandsGrid);
        commandsContent.setPadding(false);
        commandsContent.setSpacing(true);

        Details commandsDetails = new Details("Доступные команды", commandsContent);
        commandsDetails.setOpened(true);

        add(actionForm, commandsDetails, commandsCombo);
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
        commandsGrid.setSizeFull();
        commandsGrid.setHeight("300px");

        commandsGrid.addColumn(CommandResponse::getName).setHeader("Название");
        commandsGrid.addColumn(CommandResponse::getCommand).setHeader("Текст / Ключ");
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

    // ==================== Создание команды ====================

    private void openCommandCreateDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Новая команда");
        dialog.setWidth("min(900px, 92vw)");
        dialog.setMaxWidth("95vw");

        // Сброс полей
        commandNameField.clear();
        commandTextArea.clear();
        commandTypeSelect.clear();
        localHandlerSelect.clear();
        localHandlerSelect.setVisible(false);

        // Binder для создания команды
        Binder<CommandCreate> binder = new Binder<>(CommandCreate.class);

        binder.forField(commandNameField)
                .asRequired("Название команды обязательно")
                .bind(CommandCreate::getName, CommandCreate::setName);

        binder.forField(commandTextArea)
                .bind(CommandCreate::getCommand, CommandCreate::setCommand);

        binder.forField(commandTypeSelect)
                .asRequired("Тип команды обязателен")
                .bind(CommandCreate::getCommandType, CommandCreate::setCommandType);

        binder.forField(localHandlerSelect)
                .bind(cmd -> cmd.getCommand(), (cmd, value) -> cmd.setCommand(value));

        // Динамическое управление видимостью
        commandTypeSelect.addValueChangeListener(e -> updateCommandFormVisibility(e.getValue()));

        Button saveBtn = new Button("Создать", e -> {
            CommandCreate req = new CommandCreate();
            req.setVendorId(currentModel.getVendor().getId());

            if (binder.writeBeanIfValid(req)) {
                try {
                    CommandResponse created = commandService.create(req);
                    reloadCommands();
                    commandsCombo.select(created);
                    dialog.close();
                    Notification.show("Команда успешно создана", 3000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } catch (Exception ex) {
                    showError("Ошибка создания команды: " + ex.getMessage());
                }
            } else {
                showError("Заполните все обязательные поля");
            }
        });

        Button cancelBtn = new Button("Отмена", e -> dialog.close());

        VerticalLayout layout = new VerticalLayout(
                commandNameField,
                commandTypeSelect,
                commandTextArea,
                localHandlerSelect
        );
        layout.setWidthFull();
        layout.setPadding(true);
        layout.setSpacing(true);

        dialog.add(layout);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();

        // Устанавливаем начальное состояние
        updateCommandFormVisibility(commandTypeSelect.getValue());
    }

    // ==================== Редактирование команды ====================

    private void openCommandEditDialog(CommandResponse cmd) {
        if (commandService.isCommandUsedInAnyAction(cmd.getId())) {
            Notification.show("Команда используется в действиях — редактирование запрещено",
                            5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Редактирование команды: " + cmd.getName());
        dialog.setWidth("min(900px, 92vw)");
        dialog.setMaxWidth("95vw");

        commandNameField.setValue(cmd.getName());
        commandTextArea.setValue(cmd.getCommand() != null ? cmd.getCommand() : "");
        commandTypeSelect.setValue(cmd.getCommandType());
        localHandlerSelect.setValue(cmd.getCommand()); // ключ обработчика
        localHandlerSelect.setVisible(cmd.getCommandType() == CommandType.LOCAL);

        Binder<CommandUpdate> binder = new Binder<>(CommandUpdate.class);

        binder.forField(commandNameField)
                .bind(CommandUpdate::getName, CommandUpdate::setName);

        binder.forField(commandTextArea)
                .bind(CommandUpdate::getCommand, CommandUpdate::setCommand);

        binder.forField(commandTypeSelect)
                .bind(CommandUpdate::getCommandType, CommandUpdate::setCommandType);

        binder.forField(localHandlerSelect)
                .bind(cmdUpdate -> cmd.getCommand(), (cmdUpdate, value) -> {
                    // Для обновления используем command как ключ
                });

        commandTypeSelect.addValueChangeListener(e -> updateCommandFormVisibility(e.getValue()));

        Button saveBtn = new Button("Сохранить", e -> {
            CommandUpdate req = new CommandUpdate();
            if (binder.writeBeanIfValid(req)) {
                try {
                    commandService.updateCommand(cmd.getId(), req);
                    reloadCommands();
                    Notification.show("Команда обновлена")
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    dialog.close();
                } catch (Exception ex) {
                    showError("Ошибка обновления: " + ex.getMessage());
                }
            } else {
                showError("Заполните обязательные поля");
            }
        });

        Button cancelBtn = new Button("Отмена", e -> dialog.close());

        VerticalLayout layout = new VerticalLayout(
                commandNameField,
                commandTypeSelect,
                commandTextArea,
                localHandlerSelect
        );
        layout.setWidthFull();
        layout.setPadding(true);
        layout.setSpacing(true);

        dialog.add(layout);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();

        updateCommandFormVisibility(commandTypeSelect.getValue());
    }

    /** Динамическое управление видимостью полей команды */
    private void updateCommandFormVisibility(CommandType type) {
        if (type == null) {
            commandTextArea.setVisible(true);
            localHandlerSelect.setVisible(false);
            return;
        }

        boolean isLocal = type == CommandType.LOCAL;

        commandTextArea.setVisible(!isLocal);
        localHandlerSelect.setVisible(isLocal);

        commandTextArea.setRequiredIndicatorVisible(!isLocal);
        localHandlerSelect.setRequiredIndicatorVisible(isLocal);
    }

    private void confirmDeleteCommand(CommandResponse cmd) {
        if (commandService.isCommandUsedInAnyAction(cmd.getId())) {
            Notification.show("Команда используется в действиях — удаление запрещено",
                            5000, Notification.Position.MIDDLE)
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
                showError("Ошибка: " + ex.getMessage());
            }
        });

        confirm.open();
    }

    private void reloadCommands() {
        Long vendorId = currentModel.getVendor().getId();
        List<CommandResponse> filtered = commandService.getCommandsByVendor(vendorId);
        commandsCombo.setItems(filtered);
        commandsGrid.setItems(filtered);
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

    private void showSuccess(String msg) {
        Notification.show(msg, 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showError(String msg) {
        Notification.show(msg, 5000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
