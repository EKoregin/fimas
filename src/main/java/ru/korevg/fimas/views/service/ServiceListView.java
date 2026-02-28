package ru.korevg.fimas.views.service;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.korevg.fimas.dto.service.ServiceResponse;
import ru.korevg.fimas.service.PortService;
import ru.korevg.fimas.service.ServiceService;  // предполагаемое имя сервиса
import ru.korevg.fimas.views.layout.MainLayout;

import java.util.stream.Collectors;

@PageTitle("Сервисы")
@Route(value = "services", layout = MainLayout.class)
public class ServiceListView extends VerticalLayout {

    private final ServiceService serviceService;
    private final PortService portService;

    private final Grid<ServiceResponse> grid = new Grid<>(ServiceResponse.class, false);

    public ServiceListView(ServiceService serviceService, PortService portService) {
        this.serviceService = serviceService;
        this.portService = portService;

        System.out.println("Всего сервисов в базе: " + serviceService.count());

        setSizeFull();
        addClassName("service-list-view");

        configureGrid();
        add(grid);
        // В конце конструктора, после add(grid)
        grid.getDataCommunicator().setRequestedRange(0, 10);  // запрашиваем первую страницу явно
    }

    private void configureGrid() {
        grid.addThemeVariants(
                GridVariant.LUMO_ROW_STRIPES,
                GridVariant.LUMO_WRAP_CELL_CONTENT,
                GridVariant.LUMO_COMPACT
        );
        grid.setSizeFull();

        // Колонки
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

        // Колонка с портами — компактное отображение
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

        // Пагинация
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
                            .orElse(Sort.by("name").ascending());

                    Pageable pageable = PageRequest.of(page, pageSize, sort);

                    return serviceService.findAll(pageable).stream();
                },

                query -> (int) serviceService.count()
        ));

        // В configureGrid() добавь колонку действий
        grid.addComponentColumn(service -> {
            Button editBtn = new Button("Редактировать");
            editBtn.addClickListener(e -> {
                ServiceForm form = new ServiceForm(serviceService, portService);
                form.openEditDialog(service);
            });

            return editBtn;
        }).setHeader("Действия").setFlexGrow(0).setWidth("140px");

        // Кнопка создания — в конструкторе или отдельно
        Button createBtn = new Button("Создать сервис", e -> {
            ServiceForm form = new ServiceForm(serviceService, portService);
            form.openCreateDialog();
        });
        add(createBtn);
    }
}
