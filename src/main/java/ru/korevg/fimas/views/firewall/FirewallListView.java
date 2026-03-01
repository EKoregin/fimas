package ru.korevg.fimas.views.firewall;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.korevg.fimas.dto.firewall.FirewallResponse;
import ru.korevg.fimas.service.FirewallService;
import ru.korevg.fimas.service.ModelService;
import ru.korevg.fimas.views.layout.MainLayout;

@PageTitle("Firewalls")
@Route(value = "firewalls", layout = MainLayout.class)
public class FirewallListView extends VerticalLayout {

    private final FirewallService firewallService;
    private final ModelService modelService;

    private final Grid<FirewallResponse> grid = new Grid<>(FirewallResponse.class, false);

    public FirewallListView(FirewallService firewallService, ModelService modelService) {
        this.firewallService = firewallService;
        this.modelService = modelService;

        setSizeFull();
        addClassName("firewall-list-view");
        configureGrid();
        Button createBtn = new Button("Создать Firewall", e -> openCreateForm());
        add(createBtn, grid);
    }

    private void configureGrid() {
        grid.addThemeVariants(
                GridVariant.LUMO_ROW_STRIPES,
                GridVariant.LUMO_COMPACT,
                GridVariant.LUMO_WRAP_CELL_CONTENT
        );
        grid.setSizeFull();

        // Колонки
        grid.addColumn(FirewallResponse::id)
                .setHeader("ID")
                .setSortable(true)
                .setKey("id")
                .setWidth("80px")
                .setFlexGrow(0);

        grid.addColumn(FirewallResponse::name)
                .setHeader("Имя")
                .setSortable(true)
                .setKey("name")
                .setWidth("200px");

        grid.addColumn(FirewallResponse::description)
                .setHeader("Описание")
                .setFlexGrow(3);

        grid.addColumn(FirewallResponse::modelName)
                .setHeader("Модель")
                .setWidth("180px");

        grid.addColumn(FirewallResponse::vendorName)
                .setHeader("Производитель")
                .setWidth("160px");

        // Действия
        grid.addComponentColumn(fw -> {
                    Button edit = new Button(VaadinIcon.EDIT.create());
                    edit.setTooltipText("Редактировать");
                    edit.addClickListener(e -> openEditForm(fw));
                    edit.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

                    Button delete = new Button(VaadinIcon.TRASH.create());
                    delete.setTooltipText("Удалить");
                    delete.addClickListener(e -> showDeleteConfirm(fw.id()));
                    delete.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

                    HorizontalLayout layout = new HorizontalLayout(edit, delete);
                    layout.setPadding(false);
                    layout.setSpacing(false);
                    layout.setAlignItems(Alignment.CENTER);

                    return layout;
                })
                .setHeader("Действия")
                .setWidth("90px")
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.CENTER);

        // DataProvider с серверной пагинацией и сортировкой
        grid.setDataProvider(DataProvider.fromCallbacks(
                query -> {
                    int page = query.getPage();
                    int pageSize = query.getPageSize();

                    Sort sort = query.getSortOrders().stream()
                            .findFirst()
                            .map(order -> Sort.by(
                                    order.getDirection() == SortDirection.ASCENDING ? Sort.Direction.ASC : Sort.Direction.DESC,
                                    order.getSorted()))
                            .orElse(Sort.by("name").ascending());

                    Pageable pageable = PageRequest.of(page, pageSize, sort);

                    return firewallService.findAll(pageable).stream();
                },
                query -> (int) firewallService.count()
        ));
    }

    private void openEditForm(FirewallResponse response) {
        FirewallForm form = new FirewallForm(firewallService, modelService);
        form.setAfterSaveCallback(() -> grid.getDataProvider().refreshAll());
        form.openEdit(response);
    }

    private void openCreateForm() {
        FirewallForm form = new FirewallForm(firewallService, modelService);
        form.setAfterSaveCallback(() -> grid.getDataProvider().refreshAll());
        form.openCreate();
    }

    private void showDeleteConfirm(Long id) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Удалить Firewall?");
        dialog.setText("Это действие нельзя отменить. Связанные динамические адреса тоже будут удалены.");
        dialog.setConfirmText("Удалить");
        dialog.setCancelable(true);
        dialog.setCancelText("Отмена");

        dialog.addConfirmListener(e -> {
            try {
                firewallService.delete(id);
                refreshGrid();
                Notification.show("Firewall удалён", 3000, Notification.Position.TOP_CENTER);
            } catch (Exception ex) {
                Notification.show("Ошибка: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });

        dialog.open();
    }

    private void refreshGrid() {
        grid.getDataCommunicator().setRequestedRange(0, grid.getPageSize());
        grid.getDataProvider().refreshAll();
    }
}