package ru.korevg.fimas.views.port;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.formlayout.FormLayout;

@Tag("port-form")
public class PortForm extends FormLayout {

    private final Binder<PortCreateUpdateDto> binder = new Binder<>(PortCreateUpdateDto.class);

    private final TextField protocol = new TextField("Протокол");
    private final IntegerField srcPort  = new IntegerField("Исходный порт");
    private final IntegerField dstPort  = new IntegerField("Порт назначения");

    private final Button save   = new Button("Сохранить", VaadinIcon.CHECK.create());
    private final Button cancel = new Button("Отмена",   VaadinIcon.CLOSE.create());

    private Consumer<PortCreateUpdateDto> onSaveListener;
    private Runnable onCloseListener;

    public PortForm() {
        setResponsiveSteps(
                new ResponsiveStep("0", 1),
                new ResponsiveStep("500px", 2)
        );

        protocol.setRequiredIndicatorVisible(true);
        srcPort.setMin(0);
        srcPort.setMax(65535);
        dstPort.setMin(0);
        dstPort.setMax(65535);

        binder.forField(protocol)
                .asRequired("Протокол обязателен")
                .bind(PortCreateUpdateDto::getProtocol, PortCreateUpdateDto::setProtocol);

        binder.forField(srcPort)
                .asRequired("Укажите исходный порт")
                .bind(PortCreateUpdateDto::getSrcPort, PortCreateUpdateDto::setSrcPort);

        binder.forField(dstPort)
                .asRequired("Укажите порт назначения")
                .bind(PortCreateUpdateDto::getDstPort, PortCreateUpdateDto::setDstPort);

        save.addClickListener(e -> save());
        cancel.addClickListener(e -> close());

        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        add(protocol, srcPort, dstPort,
                new HorizontalLayout(save, cancel));
    }

    public void setPort(PortResponse port) {
        // если редактируем — заполняем
        // если создаём — binder.readBean(new PortCreateUpdateDto());
        if (port != null) {
            PortCreateUpdateDto dto = new PortCreateUpdateDto();
            // маппинг из PortResponse → DTO (можно через конструктор или mapper)
            dto.setProtocol(port.protocol());
            dto.setSrcPort(port.srcPort());
            dto.setDstPort(port.dstPort());
            binder.readBean(dto);
        } else {
            binder.readBean(new PortCreateUpdateDto());
        }
    }

    public void setOnSaveListener(Consumer<PortCreateUpdateDto> listener) {
        this.onSaveListener = listener;
    }

    public void setOnCloseListener(Runnable listener) {
        this.onCloseListener = listener;
    }

    private void save() {
        if (binder.validate().isOk()) {
            PortCreateUpdateDto dto = new PortCreateUpdateDto();
            binder.writeBeanIfValid(dto);
            if (onSaveListener != null) {
                onSaveListener.accept(dto);
            }
            close();
        }
    }

    private void close() {
        if (onCloseListener != null) {
            onCloseListener.run();
        }
    }
}
