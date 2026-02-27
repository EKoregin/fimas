package ru.korevg.fimas.dto.port;

import jakarta.validation.constraints.Size;
import ru.korevg.fimas.entity.Protocol;

public record PortUpdateRequest(
        Protocol protocol,
        @Size(max = 12) String srcPort,
        @Size(max = 12) String destPort
) {}