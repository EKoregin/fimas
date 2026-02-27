package ru.korevg.fimas.dto.port;

import ru.korevg.fimas.entity.Protocol;

public record PortResponse(
        Long id,
        Protocol protocol,
        String srcPort,
        String destPort
) {}