package ru.korevg.fimas.views.port;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
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
import ru.korevg.fimas.dto.port.PortResponse;
import ru.korevg.fimas.dto.service.ServiceResponse;
import ru.korevg.fimas.service.PortService;
import ru.korevg.fimas.views.layout.MainLayout;
import ru.korevg.fimas.views.service.ServiceForm;

@PageTitle("Порты")
@Route(value = "ports", layout = MainLayout.class)
public class PortListView extends VerticalLayout {

    private final PortService portService;

    private final Grid<PortResponse> grid = new Grid<>(PortResponse.class, false);

    public PortListView(PortService portService) {
        this.portService = portService;

        setSizeFull();
        addClassName("port-list-view");

        configureGrid();

        Button createBtn = new Button("Создать порт", e -> openCreatePortDialog());

        add(createBtn, grid);
    }

    private void openCreatePortDialog() {
        PortForm form = new PortForm(portService);
        form.openCreateDialog(
                newPort -> {
                    // Можно обновить грид
                    grid.getDataProvider().refreshAll();
                    Notification.show("Порт добавлен");
                },
                () -> {
                } // on cancel — можно ничего не делать
        );
    }

    private void configureGrid() {
        grid.addThemeVariants(
                GridVariant.LUMO_ROW_STRIPES,
                GridVariant.LUMO_WRAP_CELL_CONTENT,
                GridVariant.LUMO_COMPACT
        );
        grid.setSizeFull();

        // Колонки — используем методы record'а (без get)
        grid.addColumn(PortResponse::id)
                .setHeader("ID")
                .setSortable(true)
                .setKey("id")
                .setFlexGrow(0)
                .setWidth("80px");

        grid.addColumn(PortResponse::protocol)
                .setHeader("Протокол")
                .setSortable(true)
                .setKey("protocol")
                .setWidth("120px");

        grid.addColumn(PortResponse::srcPort)
                .setHeader("Исходный порт")
                .setSortable(true)
                .setKey("srcPort")
                .setWidth("130px");

        grid.addColumn(PortResponse::dstPort)
                .setHeader("Порт назначения")
                .setSortable(true)
                .setKey("dstPort")
                .setWidth("150px");

        // Пагинация
        grid.setSizeFull();

        // Серверная пагинация + поддержка сортировки по клику на заголовок
        grid.setDataProvider(DataProvider.fromCallbacks(
                query -> {
                    int page = query.getPage();
                    int pageSize = query.getPageSize();

                    // Определяем сортировку (по умолчанию — по id)
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

                    return portService.findAll(pageable).stream();
                },

                query -> (int) portService.count()
        ));

        grid.addComponentColumn(port -> {
                    Button editBtn = new Button(VaadinIcon.EDIT.create());
                    editBtn.setTooltipText("Редактировать");
                    editBtn.addClickListener(e -> openEditForm(port));
                    editBtn.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

                    Button delete = new Button(VaadinIcon.TRASH.create());
                    delete.setTooltipText("Удалить");
                    delete.addClickListener(e -> showDeleteConfirm(port.id()));
                    delete.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

                    HorizontalLayout layout = new HorizontalLayout(editBtn, delete);
                    layout.setPadding(false);
                    layout.setSpacing(false);
                    layout.setAlignItems(Alignment.CENTER);

                    return layout;
                }).setHeader("Действия")
                .setFlexGrow(0)
                .setWidth("140px")
                .setTextAlign(ColumnTextAlign.CENTER);
    }

    private void showDeleteConfirm(Long id) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Удалить порт?");
        dialog.setText("Это действие нельзя отменить.");
        dialog.setConfirmText("Удалить");

        dialog.setCancelable(true);
        dialog.setCancelText("Отмена");
        dialog.addConfirmListener(e -> {
            try {
                portService.delete(id);
                grid.getDataProvider().refreshAll();
                Notification.show("Порт удалён", 3000, Notification.Position.TOP_CENTER);
            } catch (Exception ex) {
                Notification.show("Ошибка: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });
        dialog.addCancelListener(e -> {
            Notification.show("Удаление отменено", 2000, Notification.Position.BOTTOM_START);
        });
        dialog.open();
    }

    private void openEditForm(PortResponse portResponse) {
        PortForm form = new PortForm(portService);
        form.openEditDialog(portResponse,
                updated -> grid.getDataProvider().refreshAll(),
                () -> {
                }
        );

    }
}