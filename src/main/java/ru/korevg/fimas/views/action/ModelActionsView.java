// ======================================================
// ModelActionsView – управление действиями модели (CRUD)
// ======================================================

package ru.korevg.fimas.views.action;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import lombok.extern.slf4j.Slf4j;
import ru.korevg.fimas.dto.action.ActionResponse;
import ru.korevg.fimas.dto.command.CommandResponse;
import ru.korevg.fimas.entity.Model;
import ru.korevg.fimas.exception.EntityNotFoundException;
import ru.korevg.fimas.repository.ModelRepository;
import ru.korevg.fimas.service.ActionCommandService;
import ru.korevg.fimas.views.model.ModelListView;

import java.util.List;

@Slf4j
@Route("model/:modelId/actions")
@UIScope
public class ModelActionsView extends VerticalLayout
        implements BeforeEnterObserver, HasDynamicTitle {

    private final ActionCommandService actionCommandService;
    private final ModelRepository modelRepository;

    private final Grid<ActionResponse> grid = new Grid<>(ActionResponse.class, false);
    private final H3 title = new H3();

    private Long modelId;
    private Model currentModel;

    private String dynamicTitle = "Действия модели";

    public ModelActionsView(ActionCommandService actionCommandService,
                            ModelRepository modelRepository) {
        this.actionCommandService = actionCommandService;
        this.modelRepository = modelRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        configureGrid();
        add(createToolbar(), title, grid);
    }

    private HorizontalLayout createToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(JustifyContentMode.BETWEEN);
        toolbar.setAlignItems(Alignment.CENTER);

        Button addBtn = new Button("Создать действие", VaadinIcon.PLUS.create());
        addBtn.addClickListener(e -> openActionEditor(null));

        toolbar.add(addBtn);
        return toolbar;
    }

    private void configureGrid() {
        grid.setSizeFull();

        grid.addColumn(ActionResponse::getName)
                .setHeader("Название")
                .setAutoWidth(true);

        grid.addColumn(action -> {
            List<CommandResponse> cmds = action.getCommands();
            return cmds.isEmpty() ? "— нет —" : cmds.size() + " шт.";
        }).setHeader("Команды").setAutoWidth(true);

        grid.addComponentColumn(action -> {
            HorizontalLayout buttons = new HorizontalLayout();

            Button editBtn = new Button(VaadinIcon.EDIT.create());
            editBtn.addClickListener(e -> openActionEditor(action.getId()));

            Button deleteBtn = new Button(VaadinIcon.TRASH.create());
            deleteBtn.addThemeName("error");
            deleteBtn.addClickListener(e -> confirmDelete(action));

            buttons.add(editBtn, deleteBtn);
            return buttons;
        }).setHeader("Действия").setAutoWidth(true).setFlexGrow(0);

        grid.getColumns().forEach(col -> col.setResizable(true));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        var modelIdOpt = event.getRouteParameters().get("modelId");
        if (modelIdOpt.isEmpty()) {
            showError("Отсутствует ID модели");
            return;
        }

        try {
            this.modelId = Long.parseLong(modelIdOpt.get());
        } catch (NumberFormatException e) {
            showError("Неверный формат ID модели");
            return;
        }

        loadModel();
        addNavigation();
        loadActions();
    }

    private void loadModel() {
        currentModel = modelRepository.findById(modelId)
                .orElseThrow(() -> new EntityNotFoundException("Model not found: " + modelId));

        dynamicTitle = "Действия модели — " + currentModel.getName();
        title.setText(dynamicTitle);
    }

    private void loadActions() {
        List<ActionResponse> actions = actionCommandService.getActionsByModel(modelId);
        grid.setItems(actions);
    }

    private void openActionEditor(Long actionId) {
        // Здесь должен быть диалог или отдельная view для редактирования
        // Пока — заглушка с сообщением
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(actionId == null ? "Новое действие" : "Редактирование действия");

        // TODO: добавить форму, Binder, поля name, commands и т.д.

        Button save = new Button("Сохранить", e -> {
            Notification.show("Сохранение пока не реализовано");
            dialog.close();
            loadActions(); // refresh после сохранения
        });

        Button cancel = new Button("Отмена", e -> dialog.close());

        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void confirmDelete(ActionResponse action) {
        Dialog confirm = new Dialog();
        confirm.setHeaderTitle("Удалить действие?");

        confirm.add("Вы действительно хотите удалить действие \"" + action.getName() + "\"?");

        Button yes = new Button("Удалить", e -> {
            // TODO: actionCommandService.delete(action.getId());
            Notification.show("Действие удалено (заглушка)");
            confirm.close();
            loadActions();
        });
        yes.addThemeName("error");

        Button no = new Button("Отмена", e -> confirm.close());

        confirm.getFooter().add(no, yes);
        confirm.open();
    }

    private void addNavigation() {
        Button backToModels = new Button("К списку моделей", VaadinIcon.ARROW_LEFT.create());
        backToModels.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate(ModelListView.class))
        );

        HorizontalLayout nav = new HorizontalLayout(backToModels);
        nav.setWidthFull();
        nav.setJustifyContentMode(JustifyContentMode.START);
        nav.setPadding(true);

        addComponentAsFirst(nav);
    }

    private void showError(String message) {
        Notification.show(message, 5000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    @Override
    public String getPageTitle() {
        return dynamicTitle;
    }
}