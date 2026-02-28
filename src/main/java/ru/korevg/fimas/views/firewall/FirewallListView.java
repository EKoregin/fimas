package ru.korevg.fimas.views.firewall;

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
import ru.korevg.fimas.dto.firewall.FirewallResponse;
import ru.korevg.fimas.service.FirewallService;
import ru.korevg.fimas.views.layout.MainLayout;

@PageTitle("Firewalls")
@Route(value = "firewalls", layout = MainLayout.class)
public class FirewallListView extends VerticalLayout {

    private final FirewallService firewallService;

    private final Grid<FirewallResponse> grid = new Grid<>(FirewallResponse.class, false);

    public FirewallListView(FirewallService firewallService) {
        this.firewallService = firewallService;

        setSizeFull();
        addClassName("firewall-list-view");

        configureGrid();
        add(grid);
    }

    private void configureGrid() {
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_WRAP_CELL_CONTENT);
        grid.setSizeFull();

        // Определяем колонки
        grid.addColumn(FirewallResponse::name).setHeader("Имя").setSortable(true).setKey("name");
        grid.addColumn(FirewallResponse::description).setHeader("Описание").setFlexGrow(2);
        grid.addColumn(FirewallResponse::modelName).setHeader("Модель");
        grid.addColumn(FirewallResponse::vendorName).setHeader("Производитель");
        // Можно добавить дату создания, статус и т.д., если есть в DTO

        // Включаем пагинацию
        grid.setPageSize(20);

        // Серверная пагинация и сортировка
        grid.setDataProvider(DataProvider.fromCallbacks(
                // Запрос данных для текущей страницы
                query -> {
                    int page = query.getPage();
                    int pageSize = query.getPageSize();

                    // Сортировка: берём первую активную сортировку из грида, если есть
                    Sort sort = query.getSortOrders().stream()
                            .findFirst()
                            .map(order -> Sort.by(
                                    order.getDirection() == SortDirection.ASCENDING ? Sort.Direction.ASC : Sort.Direction.DESC,
                                    order.getSorted()
                            ))
                            .orElse(Sort.by("name").ascending());

                    Pageable pageable = PageRequest.of(page, pageSize, sort);

                    return firewallService.findAll(pageable).stream();
                },

                // Общее количество элементов (для расчёта страниц)
                query -> (int) firewallService.count()
        ));
    }
}