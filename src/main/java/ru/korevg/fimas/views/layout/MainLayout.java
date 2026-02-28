package ru.korevg.fimas.views.layout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility;
import ru.korevg.fimas.views.address.AddressListView;
import ru.korevg.fimas.views.dashboard.DashboardView;
import ru.korevg.fimas.views.firewall.FirewallListView;
import ru.korevg.fimas.views.port.PortListView;
import ru.korevg.fimas.views.service.ServiceListView;

public class MainLayout extends AppLayout {

    public MainLayout() {
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 title = new H1("FIMAS — Firewall Management System");
        title.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.Margin.NONE);

        HorizontalLayout header = new HorizontalLayout(
                new DrawerToggle(),
                title
        );
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.addClassNames(LumoUtility.Padding.Horizontal.MEDIUM);

        addToNavbar(header);
    }

    private void createDrawer() {
        Tabs menu = new Tabs(
                createMenuLink("Dashboard", DashboardView.class),
                createMenuLink("Firewalls", FirewallListView.class),
//                createMenuLink("Policies", PolicyListView.class),
                createMenuLink("Addresses", AddressListView.class),
                createMenuLink("Services", ServiceListView.class),
                createMenuLink("Ports", PortListView.class)
//                createMenuLink("Vendors & Models", VendorListView.class)
        );

        menu.setOrientation(Tabs.Orientation.VERTICAL);
        menu.setWidthFull();

        VerticalLayout drawerContent = new VerticalLayout(menu);
        drawerContent.setPadding(false);
        drawerContent.setSpacing(false);

        addToDrawer(drawerContent);
    }

    private Tab createMenuLink(String text, Class<? extends Component> target) {
        RouterLink link = new RouterLink(target);
        link.setText(text);

        link.setHighlightAction((l, highlighted) -> {
            if (highlighted) {
                l.getElement().getClassList().add("active");
            } else {
                l.getElement().getClassList().remove("active");
            }
        });

        return new Tab(link);
    }
}
