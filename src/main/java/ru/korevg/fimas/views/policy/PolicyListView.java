package ru.korevg.fimas.views.policy;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.korevg.fimas.dto.RecordWithName;
import ru.korevg.fimas.dto.policy.PolicyResponse;
import ru.korevg.fimas.service.AddressService;
import ru.korevg.fimas.service.FirewallService;
import ru.korevg.fimas.service.PolicyService;
import ru.korevg.fimas.service.ServiceService;
import ru.korevg.fimas.views.firewall.FirewallListView;
import ru.korevg.fimas.views.layout.MainLayout;

import java.util.Set;
import java.util.stream.Collectors;

import static org.atmosphere.util.IOUtils.close;

@Route(value = "firewalls/:firewallId/policies", layout = MainLayout.class)
public class PolicyListView extends VerticalLayout {

    private final PolicyService policyService;
    private final FirewallService firewallService;
    private final AddressService addressService;
    private final ServiceService serviceService;

    private Long firewallId;
    private final Grid<PolicyResponse> grid = new Grid<>(PolicyResponse.class, false);

    public PolicyListView(PolicyService policyService,
                          FirewallService firewallService,
                          AddressService addressService,
                          ServiceService serviceService) {
        this.policyService = policyService;
        this.firewallService = firewallService;
        this.addressService = addressService;
        this.serviceService = serviceService;

        setSizeFull();
        addClassName("policy-list-view");

        add(grid);

        Button addBtn = new Button("Добавить политику", e -> openPolicyForm(null));
        Button cancel = new Button("Отмена", e -> {
            close();
            UI.getCurrent().navigate(FirewallListView.class);
        });
        HorizontalLayout buttons = new HorizontalLayout(addBtn, cancel);

        add(buttons);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        // Получаем параметр из URL
        String path = attachEvent.getUI().getInternals().getActiveViewLocation().getPath();
        String[] parts = path.split("/");
        if (parts.length >= 3 && parts[0].equals("firewalls")) {
            try {
                firewallId = Long.parseLong(parts[1]);
                firewallService.findById(firewallId).ifPresentOrElse(
                        fw -> {
                            H2 title = new H2("Политики для Firewall: " + fw.name());
                            addComponentAtIndex(0, title); // добавляем сверху
                        },
                        () -> Notification.show("Firewall не найден", 5000, Notification.Position.MIDDLE)
                );

                configureGrid();
                refreshGrid();
            } catch (Exception e) {
                Notification.show("Неверный ID firewall в URL", 5000, Notification.Position.MIDDLE);
            }
        } else {
            Notification.show("Неверный формат URL", 5000, Notification.Position.MIDDLE);
        }
    }

    private void configureGrid() {
        grid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
        grid.setPageSize(20);

        grid.addColumn(PolicyResponse::id)
                .setHeader("ID")
                .setWidth("80px")
                .setFlexGrow(0)
                .setAutoWidth(false);

        grid.addColumn(PolicyResponse::name)
                .setHeader("Имя")
                .setSortable(true)
                .setKey("name")
                .setWidth("180px")
                .setFlexGrow(0)
                .setAutoWidth(false);

        grid.addColumn(new ComponentRenderer<>(
                        policy -> createMultilineCell(formatSet(policy.srcAddresses()))
                ))
                .setHeader("Sources")
                .setFlexGrow(1)
                .setAutoWidth(true)
                .setResizable(true);

        grid.addColumn(new ComponentRenderer<>(
                        policy -> createMultilineCell(formatSet(policy.dstAddresses()))
                ))
                .setHeader("Destination")
                .setFlexGrow(1)
                .setAutoWidth(true)
                .setResizable(true);

        grid.addColumn(new ComponentRenderer<>(policy -> {
                    Div div = new Div();
                    div.getStyle()
                            .set("white-space", "pre-line")
                            .set("line-height", "1.4");

                    String text = formatSet(policy.services());
                    div.setText(text);

                    return div;
                }))
                .setHeader("Services")
                .setFlexGrow(1)
                .setAutoWidth(true)
                .setResizable(true);

        grid.addColumn(PolicyResponse::action)
                .setHeader("Action")
                .setSortable(true)
                .setKey("action")
                .setFlexGrow(0);
        grid.addColumn(PolicyResponse::status).setHeader("Status").setFlexGrow(0);

        grid.addComponentColumn(p -> {
            Button edit = new Button(VaadinIcon.EDIT.create());
            edit.setTooltipText("Редактировать");
            edit.addClickListener(e -> openPolicyForm(p));
            edit.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

            Button delete = new Button(VaadinIcon.TRASH.create());
            delete.setTooltipText("Удалить");
            delete.addClickListener(e -> showDeleteConfirm(p.id()));
            delete.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

            return new HorizontalLayout(edit, delete);
        }).setHeader("Действия").setWidth("90px").setFlexGrow(0).setTextAlign(ColumnTextAlign.CENTER);

        grid.setDataProvider(DataProvider.fromCallbacks(
                query -> {
                    try {
                        int page = query.getPage();
                        int pageSize = query.getPageSize();

                        Sort sort = query.getSortOrders().stream()
                                .findFirst()
                                .map(o -> Sort.by(
                                        o.getDirection() == SortDirection.ASCENDING ? Sort.Direction.ASC : Sort.Direction.DESC,
                                        o.getSorted()))
                                .orElse(Sort.by("name").ascending());

                        Pageable pageable = PageRequest.of(page, pageSize, sort);

                        return policyService.findByFirewallId(firewallId, pageable).stream();
                    } catch (Exception e) {
                        Notification.show("Ошибка загрузки данных: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
                        return java.util.stream.Stream.empty();
                    }
                },
                query -> {
                    try {
                        return (int) policyService.countByFirewallId(firewallId);
                    } catch (Exception e) {
                        Notification.show("Ошибка подсчёта: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
                        return 0;
                    }
                }
        ));
    }

    private Span createCountBadge(String label, int count) {
        Span badge = new Span(label + ": " + count);
        badge.getStyle()
                .set("background", "var(--lumo-contrast-10pct)")
                .set("padding", "2px 8px")
                .set("border-radius", "var(--lumo-border-radius-s)")
                .set("font-size", "var(--lumo-font-size-s)");
        return badge;
    }

    private void openPolicyForm(PolicyResponse existing) {
        PolicyForm form = new PolicyForm(
                policyService,
                firewallId,
                addressService,
                serviceService
        );
        form.setAfterSave(this::refreshGrid);

        if (existing != null) {
            form.edit(existing);
        } else {
            form.create();
        }
    }

    private void showDeleteConfirm(Long id) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Удалить политику?");
        dialog.setText("Действие нельзя отменить.");
        dialog.setConfirmText("Удалить");
        dialog.setCancelable(true);
        dialog.setCancelText("Отмена");

        dialog.addConfirmListener(e -> {
            try {
                policyService.delete(id);
                refreshGrid();
                Notification.show("Политика удалена", 3000, Notification.Position.TOP_CENTER);
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

    // Вспомогательный метод для красивого вывода множеств
    private String formatSet(Set<? extends RecordWithName> items) {
        if (items == null || items.isEmpty()) return "any";
        return items.stream()
                .map(a -> a.name().trim())
                .sorted()
                .collect(Collectors.joining("\n"));
    }

    private Component createMultilineCell(String text) {
        Div div = new Div();
        div.getStyle()
                .set("white-space", "pre-line")
                .set("line-height", "1.4");
        div.setText(text);
        return div;
    }
}