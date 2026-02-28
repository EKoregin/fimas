package ru.korevg.fimas.views.dashboard;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import ru.korevg.fimas.views.layout.MainLayout;

@PageTitle("Dashboard")
@Route(value = "", layout = MainLayout.class)
public class DashboardView extends VerticalLayout {

    public DashboardView() {
        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(Alignment.CENTER);

        add(new H2("Добро пожаловать в FIMAS"));
        add(new Paragraph("Управление межсетевыми экранами, политиками, адресами и сервисами"));

        // Здесь позже добавите карточки со статистикой:
        // Кол-во firewall'ов, политик, активных правил и т.д.
    }
}
