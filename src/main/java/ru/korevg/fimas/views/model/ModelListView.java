package ru.korevg.fimas.views.model;

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
import ru.korevg.fimas.dto.model.ModelResponse;
import ru.korevg.fimas.service.FirewallService;
import ru.korevg.fimas.service.ModelService;
import ru.korevg.fimas.service.VendorService;
import ru.korevg.fimas.views.layout.MainLayout;

@PageTitle("Models")
@Route(value = "models", layout = MainLayout.class)
public class ModelListView extends VerticalLayout {

    private final ModelService modelService;
    private final VendorService vendorService;

    private final Grid<ModelResponse> grid = new Grid<>(ModelResponse.class, false);

    public ModelListView(ModelService modelService, VendorService vendorService) {
        this.modelService = modelService;
        this.vendorService = vendorService;

        setSizeFull();
        addClassName("model-list-view");
        configureGrid();
        Button createBtn = new Button("Создать Model", e -> openCreateForm());
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
        grid.addColumn(ModelResponse::id)
                .setHeader("ID")
                .setSortable(true)
                .setKey("id")
                .setWidth("80px")
                .setFlexGrow(0);

        grid.addColumn(ModelResponse::name)
                .setHeader("Имя")
                .setSortable(true)
                .setKey("name")
                .setWidth("200px");

        grid.addColumn(ModelResponse::vendorName)
                .setHeader("Производитель")
                .setFlexGrow(3);


        grid.addComponentColumn(modelResponse -> new Button("Действия", e ->
                getUI().ifPresent(ui -> ui.navigate("model/" + modelResponse.id() + "/actions"))
        )).setHeader("Actions").setWidth("120px");

        // Действия
        grid.addComponentColumn(modelResponse -> {
                    Button edit = new Button(VaadinIcon.EDIT.create());
                    edit.setTooltipText("Редактировать");
                    edit.addClickListener(e -> openEditForm(modelResponse));
                    edit.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

                    Button delete = new Button(VaadinIcon.TRASH.create());
                    delete.setTooltipText("Удалить");
                    delete.addClickListener(e -> showDeleteConfirm(modelResponse.id()));
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

                    return modelService.findAll(pageable).stream();
                },
                query -> (int) modelService.count()
        ));
    }

    private void openCreateForm() {
        ModelForm form = new ModelForm(modelService, vendorService);
        form.setAfterSaveCallback(v -> refreshGrid());
        form.openCreate();
    }

    private void openEditForm(ModelResponse response) {
        ModelForm form = new ModelForm(modelService, vendorService);
        form.setAfterSaveCallback(v -> refreshGrid());
        form.openEdit(response);
    }

    private void showDeleteConfirm(Long id) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Удалить Model?");
        dialog.setText("Это действие нельзя отменить. Связанные действия будут удалены");
        dialog.setConfirmText("Удалить");
        dialog.setCancelable(true);
        dialog.setCancelText("Отмена");

        dialog.addConfirmListener(e -> {
            try {
                modelService.delete(id);
                refreshGrid();
                Notification.show("Model удалёна", 3000, Notification.Position.TOP_CENTER);
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