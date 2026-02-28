package ru.korevg.fimas.dto.port;

import jakarta.validation.constraints.Size;
import ru.korevg.fimas.entity.Protocol;
import ru.korevg.fimas.validation.ValidPortRange;
import ru.korevg.fimas.validation.ValueOfEnum;

public record PortUpdateRequest(

        @ValueOfEnum(
                enumClass = Protocol.class
        )
        String protocol,

        @ValidPortRange
        @Size(max = 12)
        String srcPort,

        @ValidPortRange
        @Size(max = 12)
        String dstPort
) {}