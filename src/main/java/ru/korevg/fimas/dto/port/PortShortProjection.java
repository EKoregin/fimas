package ru.korevg.fimas.dto.port;

public interface PortShortProjection {
    Long getId();
    String getProtocol();
    String getSrcPort();
    String getDstPort();
}
