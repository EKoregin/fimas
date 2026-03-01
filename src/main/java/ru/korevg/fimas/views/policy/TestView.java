// src/main/java/ru/korevg/fimas/views/TestView.java
package ru.korevg.fimas.views.policy;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("test-view")
public class TestView extends VerticalLayout {

    public TestView() {
        System.out.println("TestView СОЗДАН И ЗАРЕГИСТРИРОВАН");
        add(new H1("Тестовый роут работает!"));
    }
}