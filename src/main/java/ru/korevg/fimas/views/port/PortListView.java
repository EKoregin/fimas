package ru.korevg.fimas.views.port;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.korevg.fimas.dto.port.PortResponse;
import ru.korevg.fimas.service.PortService;
import ru.korevg.fimas.views.layout.MainLayout;

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
        add(grid);
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
    }
}