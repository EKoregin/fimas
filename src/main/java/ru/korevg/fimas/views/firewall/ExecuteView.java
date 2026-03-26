package ru.korevg.fimas.views.firewall;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.progressbar.ProgressBar;

@Route("execute")           // адрес страницы: http://localhost:8080/execute
@PageTitle("Выполнить задачу")
public class ExecuteView extends VerticalLayout {

    public ExecuteView() {
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSizeFull();

        Button executeButton = new Button("Выполнить", new Icon(VaadinIcon.PLAY));
        executeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        executeButton.setWidth("250px");

        executeButton.addClickListener(event -> {
            executeWithSimulation(executeButton);
        });

        add(executeButton);
    }

    private void executeWithSimulation(Button button) {
        // Отключаем кнопку на время выполнения, чтобы избежать повторных кликов
        button.setEnabled(false);
        button.setText("Выполняется...");

        // Показываем индикатор прогресса (можно заменить на Notification или overlay)
        ProgressBar progressBar = new ProgressBar();
        progressBar.setIndeterminate(true);
        add(progressBar);

        // Эмуляция долгой работы (в реальном проекте — запуск в отдельном потоке)
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                try {
                    // Симуляция работы 2 секунды
                    Thread.sleep(2000);

                    // Убираем прогресс-бар
                    remove(progressBar);

                    // Открываем модальное окно с результатом
                    showSuccessDialog();

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    // Восстанавливаем кнопку
                    button.setEnabled(true);
                    button.setText("Выполнить");
                    button.setIcon(new Icon(VaadinIcon.PLAY));
                }
            });
        });
    }

    private void showSuccessDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Операция завершена");
//        dialog.setModal(true);                    // модальное окно
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(false);     // нельзя закрыть кликом вне окна

        VerticalLayout content = new VerticalLayout();
        content.setAlignItems(Alignment.CENTER);

        Icon successIcon = new Icon(VaadinIcon.CHECK_CIRCLE);
        successIcon.setColor("var(--lumo-success-color)");
        successIcon.setSize("48px");

        Paragraph message = new Paragraph("Задача успешно выполнена!");
        message.getStyle().set("font-size", "18px");

        content.add(successIcon, message);

        dialog.add(content);

        // Кнопка в футере
        Button closeButton = new Button("Закрыть", e -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(closeButton);

        dialog.open();
    }
}