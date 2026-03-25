
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
import com.vaadin.flow.data.renderer.ComponentRenderer;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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
            Set<CommandResponse> cmds = action.getCommands();
            return cmds.isEmpty() ? "— нет команд —" : cmds.size() + " команд(ы)";
        }).setHeader("Команды").setAutoWidth(true);

        grid.addComponentColumn(action -> {
                    Button btn = new Button("Выполнить", e -> executeAction(action));
                    btn.addThemeName("primary");
                    btn.setWidthFull();

                    actionButtons.put(action.getId(), btn);

                    return btn;
                })
                .setHeader("Выполнить")
                .setKey("execute")
                .setAutoWidth(true)
                .setFlexGrow(0);

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
        Button btn = actionButtons.get(action.getId());
        if (btn == null) {
            showError("Кнопка выполнения не найдена");
            return;
        }

        btn.setEnabled(false);
        btn.setText("Выполняется...");
        log.info("Кнопка переведена в состояние 'Выполняется...' для actionId={}", action.getId());

        ProgressBar progress = new ProgressBar();
        progress.setIndeterminate(true);
        progress.setWidth("100%");

        AtomicLong seconds = new AtomicLong(0);
        Span timer = new Span("0 сек");
        timer.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-primary-text-color)")
                .set("margin-top", "4px");

        VerticalLayout indicator = new VerticalLayout(btn, progress, timer);
        indicator.setPadding(false);
        indicator.setSpacing(false);
        indicator.setAlignItems(Alignment.CENTER);

        var column = grid.getColumnByKey("execute");
        log.info("Меняем рендерер на PROGRESS для actionId={}", action.getId());

        column.setRenderer(
                new ComponentRenderer<>(
                        (ActionResponse item) -> {
                            log.info("Renderer called for item {}", item.getId());
                            if (item.getId().equals(action.getId())) {
                                log.info("Return indicator");
                                return indicator;
                            }
                            Button originalBtn = actionButtons.get(item.getId());
                            return originalBtn != null ? originalBtn : btn;
                        }
                )
        );
        grid.getDataCommunicator().reset();
        grid.getDataProvider().refreshAll();
//        grid.getGenericDataView().refreshAll();
        log.info("refreshAll() вызван (progress)");

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable tick = () -> getUI().ifPresent(ui -> ui.access(() ->
                timer.setText(seconds.incrementAndGet() + " сек")
        ));
        ScheduledFuture<?> timerTask = scheduler.scheduleAtFixedRate(tick, 1, 1, TimeUnit.SECONDS);

        CompletableFuture.supplyAsync(() -> {
            try {
                return executionService.executeActionOnModel(
                        currentModel,
                        action.getId(),
                        currentFirewall.getMgmtIpAddress(),
                        "admin",
                        "password"
                );
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }).thenAccept(results -> getUI().ifPresent(ui -> ui.access(() -> {
            timerTask.cancel(false);
            scheduler.shutdownNow();
            btn.setEnabled(true);
            btn.setText("Выполнить");

            log.info("Выполняется блок ThenAccept");

            grid.getColumnByKey("execute").setRenderer(
                    new ComponentRenderer<>(
                            (ActionResponse item) -> {
                                log.info("Renderer called for item {}", item.getId());
                                Button originalBtn = actionButtons.get(item.getId());
                                return originalBtn != null ? originalBtn : btn;
                            }
                    )
            );

            Notification.show("Действие успешно выполнено (" + seconds.get() + " сек)", 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            showResultDialog(action.getName(), String.join("\n\n", results));
        }))).exceptionally(ex -> {
            getUI().ifPresent(ui -> ui.access(() -> {
                timerTask.cancel(false);
                scheduler.shutdownNow();
                btn.setEnabled(true);
                btn.setText("Выполнить");

                log.info("Выполняется блок Exceptionally");
                var updateColumn = grid.getColumnByKey("execute");
                log.info("Меняем рендерер обратно на BUTTONS");
                updateColumn.setRenderer(
                        new ComponentRenderer<>(
                                (ActionResponse item) -> {
                                    log.info("Renderer called for item {}", item.getId());
                                    Button originalBtn = actionButtons.get(item.getId());
                                    return originalBtn != null ? originalBtn : btn;
                                }
                        )
                );
                grid.getDataCommunicator().reset();
                grid.getDataProvider().refreshAll();
//                grid.getGenericDataView().refreshAll();
                log.info("refreshAll() вызван (restore buttons)");

                String errorMsg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                Notification.show("Ошибка выполнения (" + seconds.get() + " сек): " + errorMsg, 10000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);

                Dialog errorDialog = new Dialog();
                errorDialog.setHeaderTitle("Ошибка");
                errorDialog.add(new Span(errorMsg));
                Button close = new Button("Закрыть", e -> errorDialog.close());
                errorDialog.add(close);
                errorDialog.open();
            }));
            return null;
        });
    }


    private void showResultDialog(String actionName, String result) {
        Dialog dialog = new Dialog();
        dialog.setWidth("900px");
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
