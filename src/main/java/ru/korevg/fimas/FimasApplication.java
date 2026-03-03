package ru.korevg.fimas;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.aura.Aura;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"ru.korevg.fimas"})
@StyleSheet(Aura.STYLESHEET)
public class FimasApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(FimasApplication.class, args);
    }
}
