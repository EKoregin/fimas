// ======================================================
// FirewallActionsView – выполнение действий на конкретном firewall
// ======================================================

package ru.korevg.fimas.views.action;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
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
import ru.korevg.fimas.entity.Firewall;
import ru.korevg.fimas.entity.Model;
import ru.korevg.fimas.exception.EntityNotFoundException;
import ru.korevg.fimas.repository.FirewallRepository;
import ru.korevg.fimas.repository.ModelRepository;
import ru.korevg.fimas.service.ActionCommandService;
import ru.korevg.fimas.service.FirewallExecutionService;
import ru.korevg.fimas.views.model.ModelListView;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Route("model/:modelId/actions/firewall/:fwId")
@UIScope
public class FirewallActionsView extends VerticalLayout
        implements BeforeEnterObserver, HasDynamicTitle {

    private final ActionCommandService actionCommandService;
    private final FirewallExecutionService executionService;
    private final ModelRepository modelRepository;
    private final FirewallRepository firewallRepository;

    private final Grid<ActionResponse> grid = new Grid<>(ActionResponse.class, false);
    private final H3 title = new H3();

    private Long modelId;
    private Long fwId;
    private Model currentModel;
    private Firewall currentFirewall;

    private String dynamicTitle = "Действия для Firewall";

    public FirewallActionsView(ActionCommandService actionCommandService,
                               FirewallExecutionService executionService,
                               ModelRepository modelRepository,
                               FirewallRepository firewallRepository) {
        this.actionCommandService = actionCommandService;
        this.executionService = executionService;
        this.modelRepository = modelRepository;
        this.firewallRepository = firewallRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        configureGrid();
        add(title, grid);
    }

    private void configureGrid() {
        grid.setSizeFull();

        grid.addColumn(ActionResponse::getName)
                .setHeader("Название действия")
                .setAutoWidth(true);

        grid.addColumn(action -> {
            List<CommandResponse> cmds = action.getCommands();
            return cmds.isEmpty() ? "— нет команд —" : cmds.size() + " команд(ы)";
        }).setHeader("Команды").setAutoWidth(true);

        grid.addComponentColumn(action -> {
            Button btn = new Button("Выполнить", e -> executeAction(action));
            btn.addThemeName("primary");
            btn.setWidthFull();
            return btn;
        }).setHeader("Выполнить").setAutoWidth(true).setFlexGrow(0);

        grid.getColumns().forEach(col -> col.setResizable(true));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        var modelIdOpt = event.getRouteParameters().get("modelId");
        var fwIdOpt = event.getRouteParameters().get("fwId");

        if (modelIdOpt.isEmpty() || fwIdOpt.isEmpty()) {
            showError("Отсутствуют обязательные параметры");
            return;
        }

        try {
            this.modelId = Long.parseLong(modelIdOpt.get());
            this.fwId = Long.parseLong(fwIdOpt.get());
        } catch (NumberFormatException e) {
            showError("Неверный формат ID");
            return;
        }

        loadModelAndFirewall();
        addNavigation();
        loadActions();
    }

    private void loadModelAndFirewall() {
        currentModel = modelRepository.findById(modelId)
                .orElseThrow(() -> new EntityNotFoundException("Model not found: " + modelId));

        currentFirewall = firewallRepository.findById(fwId)
                .orElseThrow(() -> new EntityNotFoundException("Firewall not found: " + fwId));

        dynamicTitle = "Действия — " + currentFirewall.getName() +
                " / " + currentModel.getVendor().getName() + ": " + currentModel.getName();
        title.setText(dynamicTitle);
    }

    private void loadActions() {
        List<ActionResponse> actions = actionCommandService.getActionsByModel(modelId);
        grid.setItems(actions);
    }

    private void executeAction(ActionResponse action) {
        Notification.show("Выполнение: " + action.getName() + "...", 3000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_PRIMARY);

        CompletableFuture.supplyAsync(() -> {
            try {
                return executionService.executeActionOnModel(
                        currentModel,
                        action.getId(),
                        currentFirewall.getMgmtIpAddress(),
                        "admin",      // ← вынести в форму/настройки
                        "password"
                );
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }).thenAccept(results -> getUI().ifPresent(ui -> ui.access(() -> {
            Notification.show("Действие выполнено успешно!", 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            showResultDialog(action.getName(), String.join("\n\n", results));
        }))).exceptionally(ex -> {
            getUI().ifPresent(ui -> ui.access(() ->
                    showError("Ошибка: " + ex.getCause().getMessage())
            ));
            return null;
        });
    }

    private void showResultDialog(String actionName, String result) {
        Dialog dialog = new Dialog();
        dialog.setWidth("850px");
        dialog.setHeight("650px");

        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();

        H3 header = new H3("Результат: " + actionName);
        Span text = new Span(result);
        text.getStyle()
                .set("white-space", "pre-wrap")
                .set("font-family", "monospace")
                .set("overflow", "auto");

        Button close = new Button("Закрыть", e -> dialog.close());

        content.add(header, text, close);
        dialog.add(content);
        dialog.open();
    }

    private void addNavigation() {
        Button backToFirewallList = new Button("Список firewall", VaadinIcon.ARROW_LEFT.create());
        backToFirewallList.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate(
                        "firewalls"
                ))
        );

        Button backToModels = new Button("Модели", VaadinIcon.LIST.create());
        backToModels.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate(ModelListView.class))
        );

        HorizontalLayout nav = new HorizontalLayout(backToFirewallList, backToModels);
        nav.setWidthFull();
        nav.setJustifyContentMode(JustifyContentMode.START);
        nav.setPadding(true);

        addComponentAsFirst(nav);
    }

    private void showError(String message) {
        Notification notification = Notification.show(message, 5000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    @Override
    public String getPageTitle() {
        return dynamicTitle;
    }
}