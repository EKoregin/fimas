package ru.korevg.fimas.views.address;

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
import ru.korevg.fimas.dto.address.AddressResponse;
import ru.korevg.fimas.service.AddressService;
import ru.korevg.fimas.views.layout.MainLayout;

import java.util.stream.Collectors;

@PageTitle("Адреса")
@Route(value = "addresses", layout = MainLayout.class)
public class AddressListView extends VerticalLayout {

    private final AddressService addressService;

    private final Grid<AddressResponse> grid = new Grid<>(AddressResponse.class, false);

    public AddressListView(AddressService addressService) {
        this.addressService = addressService;

        setSizeFull();
        addClassName("address-list-view");

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

        // Колонки
        grid.addColumn(AddressResponse::id)
                .setHeader("ID")
                .setSortable(true)
                .setKey("id")
                .setFlexGrow(0)
                .setWidth("80px");

        grid.addColumn(AddressResponse::addressType)
                .setHeader("Тип")
                .setSortable(true)
                .setKey("addressType")
                .setWidth("110px")
                .setFlexGrow(0);

        grid.addColumn(AddressResponse::name)
                .setHeader("Имя")
                .setSortable(true)
                .setKey("name")
                .setWidth("180px");

        grid.addColumn(AddressResponse::description)
                .setHeader("Описание")
                .setFlexGrow(3);

        // Колонка с адресами (IP, подсети и т.д.)
        grid.addComponentColumn(addr -> {
                    if (addr.addresses() == null || addr.addresses().isEmpty()) {
                        return new Span("—");
                    }

                    String text = addr.addresses().stream()
                            .collect(Collectors.joining(", "));

                    Span span = new Span(text);
                    span.getStyle()
                            .set("white-space", "nowrap")
                            .set("overflow", "hidden")
                            .set("text-overflow", "ellipsis");

                    return span;
                })
                .setHeader("Адреса")
                .setKey("addresses")
                .setFlexGrow(3)
                .setSortable(false);

        // Колонка Firewall (только для DYNAMIC)
        grid.addColumn(addr -> {
                    if ("DYNAMIC".equals(addr.addressType())) {
                        return addr.firewallName() != null ? addr.firewallName() : "—";
                    }
                    return "—";
                })
                .setHeader("Firewall")
                .setKey("firewallName")
                .setSortable(true)
                .setFlexGrow(2)
                .setWidth("180px");

        // Пагинация
        grid.setPageSize(25);

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

                    return addressService.findAll(pageable).stream();
                },

                query -> (int) addressService.count()
        ));
    }
}
