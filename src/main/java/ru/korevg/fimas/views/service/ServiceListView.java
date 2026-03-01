package ru.korevg.fimas.views.service;

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
import ru.korevg.fimas.dto.service.ServiceResponse;
import ru.korevg.fimas.service.AddressService;
import ru.korevg.fimas.service.PortService;
import ru.korevg.fimas.service.ServiceService;
import ru.korevg.fimas.views.layout.MainLayout;

import java.util.stream.Collectors;

@PageTitle("Сервисы")
@Route(value = "services", layout = MainLayout.class)
public class ServiceListView extends VerticalLayout {

    private final ServiceService serviceService;
    private final PortService portService;

    private final Grid<ServiceResponse> grid = new Grid<>(ServiceResponse.class, false);

    public ServiceListView(ServiceService serviceService, PortService portService, AddressService addressService) {
        this.serviceService = serviceService;
        this.portService = portService;

        setSizeFull();
        addClassName("service-list-view");
        configureGrid();

        Button createBtn = new Button("Создать сервис", e -> openCreateForm());
        add(createBtn);
        add(grid);
    }

    private void configureGrid() {
        grid.addThemeVariants(
                GridVariant.LUMO_ROW_STRIPES,
                GridVariant.LUMO_WRAP_CELL_CONTENT,
                GridVariant.LUMO_COMPACT
        );
        grid.setSizeFull();

        grid.addColumn(ServiceResponse::id)
                .setHeader("ID")
                .setSortable(true)
                .setKey("id")
                .setFlexGrow(0)
                .setWidth("80px");

        grid.addColumn(ServiceResponse::name)
                .setHeader("Имя")
                .setSortable(true)
                .setKey("name")
                .setWidth("180px");

        grid.addColumn(ServiceResponse::description)
                .setHeader("Описание")
                .setFlexGrow(3);

        grid.addComponentColumn(service -> {
                    if (service.ports() == null || service.ports().isEmpty()) {
                        return new Span("—");
                    }

                    String portsText = service.ports().stream()
                            .map(p -> String.format("%s %s→%s",
                                    p.getProtocol(),
                                    p.getSrcPort() != null ? p.getSrcPort() : "*",
                                    p.getDstPort() != null ? p.getDstPort() : "*"))
                            .collect(Collectors.joining(", "));

                    Span span = new Span(portsText);
                    span.getStyle()
                            .set("white-space", "nowrap")
                            .set("overflow", "hidden")
                            .set("text-overflow", "ellipsis");

                    return span;
                })
                .setHeader("Порты")
                .setKey("ports")
                .setFlexGrow(2)
                .setSortable(false);  // сортировка по коллекции обычно не нужна

        grid.setSizeFull();

        // Серверная пагинация + сортировка
        grid.setDataProvider(DataProvider.fromCallbacks(
                query -> {
                    int page = query.getPage();
                    int pageSize = query.getPageSize();

                    Sort sort = query.getSortOrders().stream()
                            .findFirst()
                            .map(order -> Sort.by(
                                    order.getDirection() == SortDirection.ASCENDING
                                            ? Sort.Direction.ASC
                                            : Sort.Direction.DESC,
                                    order.getSorted()
                            ))
                            .orElse(Sort.by("id").ascending());

                    Pageable pageable = PageRequest.of(page, pageSize, sort);

                    return serviceService.findAll(pageable).stream();
                },

                query -> (int) serviceService.count()
        ));

        grid.addComponentColumn(service -> {
                    Button editBtn = new Button(VaadinIcon.EDIT.create());
                    editBtn.setTooltipText("Редактировать");
                    editBtn.addClickListener(e -> openEditForm(service));
                    editBtn.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

                    Button delete = new Button(VaadinIcon.TRASH.create());
                    delete.setTooltipText("Удалить");
                    delete.addClickListener(e -> showDeleteConfirm(service.id()));
                    delete.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

                    HorizontalLayout layout = new HorizontalLayout(editBtn, delete);
                    layout.setPadding(false);
                    layout.setSpacing(false);           // или очень маленькое: layout.setSpacing(Size.SMALL);
                    layout.setAlignItems(Alignment.CENTER);

                    return layout;
                }).setHeader("Действия")
                .setFlexGrow(0)
                .setWidth("140px")
                .setTextAlign(ColumnTextAlign.CENTER);
    }

    private void openEditForm(ServiceResponse service) {
        ServiceForm form = new ServiceForm(serviceService, portService);
        form.setAfterSaveCallback(() -> grid.getDataProvider().refreshAll());
        form.openEditDialog(service);
    }

    private void openCreateForm() {
        ServiceForm form = new ServiceForm(serviceService, portService);
        form.setAfterSaveCallback(() -> grid.getDataProvider().refreshAll());
        form.openCreateDialog();
    }

    private void showDeleteConfirm(Long id) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Удалить сервис?");
        dialog.setText("Это действие нельзя отменить.");
        dialog.setConfirmText("Удалить");

        dialog.setCancelable(true);
        dialog.setCancelText("Отмена");
        dialog.addConfirmListener(e -> {
            try {
                serviceService.delete(id);
                grid.getDataProvider().refreshAll();
                Notification.show("Сервис удалён", 3000, Notification.Position.TOP_CENTER);
            } catch (Exception ex) {
                Notification.show("Ошибка: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });
        dialog.addCancelListener(e -> {
            Notification.show("Удаление отменено", 2000, Notification.Position.BOTTOM_START);
        });
        dialog.open();
    }
}
