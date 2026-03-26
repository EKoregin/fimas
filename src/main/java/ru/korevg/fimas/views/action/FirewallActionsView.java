
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
import com.vaadin.flow.component.progressbar.ProgressBar;
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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
    private final Map<Long, Button> actionButtons = new ConcurrentHashMap<>();


    private Long modelId;
    private Long fwId;
    private Model currentModel;
    private Firewall currentFirewall;

    private String dynamicTitle = "Действия для Firewall: ";

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
        add(new H3(""), grid);
    }

    private void configureGrid() {
        grid.setSizeFull();

        grid.addColumn(ActionResponse::getName)
                .setHeader("Название действия")
                .setAutoWidth(true);

        grid.addComponentColumn(action -> {
                    Set<CommandResponse> cmds = action.getCommands();

                    if (cmds == null || cmds.isEmpty()) {
                        Span empty = new Span("— нет команд —");
                        empty.getStyle().set("color", "var(--lumo-secondary-text-color)");
                        return empty;
                    }

                    VerticalLayout namesLayout = new VerticalLayout();
                    namesLayout.setPadding(false);
                    namesLayout.setSpacing(false);
                    namesLayout.setMargin(false);

                    cmds.stream()
                            .map(CommandResponse::getName)
                            .sorted(String::compareToIgnoreCase)   // алфавитный порядок
                            .forEach(name -> {
                                Span span = new Span(name);
                                span.getStyle()
                                        .set("display", "block")
                                        .set("white-space", "normal")
                                        .set("line-height", "1.35");
                                namesLayout.add(span);
                            });

                    return namesLayout;
                })
                .setHeader("Команды")
                .setAutoWidth(true)
                .setFlexGrow(1)
                .setResizable(true);

        grid.addComponentColumn(action -> createExecuteComponent(action))
                .setHeader("Выполнить")
                .setKey("execute")
                .setAutoWidth(true)
                .setFlexGrow(0);
    }

    private VerticalLayout createExecuteComponent(ActionResponse action) {
        Button btn = new Button("Выполнить", e -> executeAction(action));
        btn.addThemeName("primary");
        btn.setWidthFull();

        actionButtons.put(action.getId(), btn);

        VerticalLayout wrapper = new VerticalLayout(btn);
        wrapper.setPadding(false);
        wrapper.setMargin(false);
        wrapper.setSpacing(false);
        wrapper.setWidthFull();

        return wrapper;
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

        dynamicTitle = dynamicTitle + currentFirewall.getName() +
                " / " + currentModel.getVendor().getName() + ": " + currentModel.getName();
        getChildren().filter(c -> c instanceof H3)
                .findFirst()
                .ifPresent(h -> ((H3) h).setText(dynamicTitle));
    }

    private void loadActions() {
        List<ActionResponse> actions = actionCommandService.getActionsByModel(modelId);
        grid.setItems(actions);
    }

    private void restoreButton(Button btn) {
        btn.setEnabled(true);
        btn.setText("Выполнить");
        btn.setIcon(null);
    }

    private void executeAction(ActionResponse action) {
        Button btn = actionButtons.get(action.getId());
        if (btn == null) {
            showError("Кнопка выполнения не найдена");
            return;
        }

        ProgressBar progress = new ProgressBar();
        progress.setIndeterminate(true);
        progress.setWidth("100%");

        try {
            List<String> results = executionService.executeActionOnModel(
                    currentModel,
                    action.getId(),
                    currentFirewall.getMgmtIpAddress(),
                    "admin",
                    "password"
            );
            showResultDialog(action.getName(), String.join("\n\n", results));
        } catch (Exception e) {
            showErrorDialog("Ошибка выполнения", e.getMessage());
        } finally {
            btn.setEnabled(true);
            btn.setText("Выполнить");
            btn.setIcon(null);
        }
    }

    private void showResultDialog(String actionName, String result) {
        Dialog dialog = new Dialog();
        dialog.setWidth("900px");
        dialog.setResizable(true);
        dialog.setHeight("700px");

        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();

        H3 header = new H3("Результат выполнения: " + actionName);
        Span text = new Span(result);
        text.getStyle()
                .set("white-space", "pre-wrap")
                .set("font-family", "monospace")
                .set("overflow", "auto")
                .set("padding", "10px")
                .set("background", "#f8f8f8")
                .set("border", "1px solid #ddd");

        Button close = new Button("Закрыть", e -> dialog.close());
        close.addThemeName("primary");

        content.add(header, text, close);
        dialog.add(content);
        dialog.open();
    }

    private void showErrorDialog(String title, String message) {
        Dialog errorDialog = new Dialog();
        errorDialog.setHeaderTitle(title);

        Span errorText = new Span(message);
        errorText.getStyle().set("color", "var(--lumo-error-text-color)");

        Button close = new Button("Закрыть", e -> errorDialog.close());

        errorDialog.add(new VerticalLayout(errorText, close));
        errorDialog.open();
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
