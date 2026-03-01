package ru.korevg.fimas.views.address;

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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.korevg.fimas.dto.address.AddressResponse;
import ru.korevg.fimas.service.AddressService;
import ru.korevg.fimas.service.FirewallService;
import ru.korevg.fimas.views.layout.MainLayout;

@PageTitle("Адреса")
@Route(value = "addresses", layout = MainLayout.class)
public class AddressListView extends VerticalLayout {

    private final AddressService addressService;
    private final FirewallService firewallService;
    private final TextField searchField;

    private final Grid<AddressResponse> grid = new Grid<>(AddressResponse.class, false);

    public AddressListView(AddressService addressService, FirewallService firewallService) {
        this.addressService = addressService;
        this.firewallService = firewallService;

        setSizeFull();
        addClassName("address-list-view");

        configureGrid();

        searchField = new TextField();
        searchField.setPlaceholder("Поиск по имени, адресу, firewall...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setWidth("350px");

        Button clearBtn = new Button("Очистить", e -> {
            searchField.clear();
            refreshGrid();
        });
        clearBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout searchLayout = new HorizontalLayout(searchField, clearBtn);
        searchLayout.setAlignItems(Alignment.BASELINE);
        searchLayout.setPadding(true);

        Button createBtn = new Button("Создать адрес", e -> openCreateForm());

        add(searchLayout, createBtn, grid);

        searchField.addValueChangeListener(e -> {
            if (e.isFromClient()) {  // только пользовательский ввод
                refreshGrid();
            }
        });
    }

    private String currentSearch = null;

    private void refreshGrid() {
        currentSearch = searchField.getValue().trim();
        if (currentSearch.isEmpty()) {
            currentSearch = null;
        }
        grid.getDataCommunicator().setRequestedRange(0, grid.getPageSize()); // на первую страницу
        grid.getDataProvider().refreshAll();
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
                .setWidth("80px")
                .setFlexGrow(0);

        grid.addColumn(addressResponse -> {
                    if (addressResponse.addressType() == null) return "—";
                    return switch (addressResponse.addressType()) {
                        case "COMMON" -> "Статический";
                        case "DYNAMIC" -> "Динамический";
                        default -> addressResponse.addressType(); // на случай новых типов
                    };
                })
                .setHeader("Тип")
                .setSortable(true)
                .setKey("addressType")
                .setWidth("130px")
                .setFlexGrow(0);

        grid.addColumn(AddressResponse::name)
                .setHeader("Имя")
                .setSortable(true)
                .setKey("name")
                .setWidth("180px")
                .setFlexGrow(0);

        grid.addComponentColumn(resp -> {
            if (resp.addresses() == null || resp.addresses().isEmpty()) {
                return new Span("—");
            }
            String text = String.join(", ", resp.addresses());
            Span span = new Span(text);
            span.getStyle()
                    .set("white-space", "nowrap")
                    .set("overflow", "hidden")
                    .set("text-overflow", "ellipsis")
                    .set("max-width", "300px");
            return span;
        }).setHeader("Адреса").setFlexGrow(3).setSortable(false);

        grid.addColumn(resp -> resp.firewallName() != null ? resp.firewallName() : "—")
                .setHeader("Firewall")
                .setSortable(false);

        grid.addComponentColumn(resp -> {
                    Button edit = new Button(VaadinIcon.EDIT.create());
                    edit.setTooltipText("Редактировать");
                    edit.addClickListener(e -> openEditForm(resp));
                    edit.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

                    Button delete = new Button(VaadinIcon.TRASH.create());
                    delete.setTooltipText("Удалить");
                    delete.addClickListener(e -> showDeleteConfirm(resp.id()));
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
                                    order.getSorted()))
                            .orElse(Sort.by("id").descending()); // или .ascending()

                    Pageable pageable = PageRequest.of(page, pageSize, sort);

                    // ← здесь передаём поисковый запрос
                    return addressService.findAll(pageable, currentSearch).stream();
                },

                query -> (int) addressService.count(currentSearch)
        ));
//        grid.setDataProvider(DataProvider.fromCallbacks(
//                query -> {
//                    int page = query.getPage();
//                    int pageSize = query.getPageSize();
//
//                    Sort sort = query.getSortOrders().stream()
//                            .findFirst()
//                            .map(order -> Sort.by(
//                                    order.getDirection() == SortDirection.ASCENDING
//                                            ? Sort.Direction.ASC
//                                            : Sort.Direction.DESC,
//                                    order.getSorted()
//                            ))
//                            .orElse(Sort.by("id").ascending());
//
//                    Pageable pageable = PageRequest.of(page, pageSize, sort);
//
//                    return addressService.findAll(pageable).stream();
//                },
//
//                query -> (int) addressService.count()
//        ));
    }

    private void openCreateForm() {
        AddressForm form = new AddressForm(addressService, firewallService);
        form.setAfterSaveCallback(this::refreshGrid);
        form.openCreate();
    }

    private void openEditForm(AddressResponse response) {
        AddressForm form = new AddressForm(addressService, firewallService);
        form.setAfterSaveCallback(this::refreshGrid);
        form.openEdit(response);
    }

    private void showDeleteConfirm(Long id) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Удалить адрес?");
        dialog.setText("Это действие нельзя отменить.");
        dialog.setConfirmText("Удалить");
        dialog.setCancelable(true);
        dialog.setCancelText("Отмена");
        dialog.addConfirmListener(e -> {
            try {
                addressService.delete(id);
                refreshGrid();
                Notification.show("Адрес удалён", 3000, Notification.Position.MIDDLE);
            } catch (Exception ex) {
                Notification.show("Ошибка: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });
        dialog.open();
    }
}
