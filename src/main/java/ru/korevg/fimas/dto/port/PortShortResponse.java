package ru.korevg.fimas.dto.port;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PortShortResponse {
    private Long id;
    private String protocol;
    private String srcPort;
    private String dstPort;

    public PortShortResponse(Long id, String protocol, String srcPort, String dstPort) {
        this.id = id;
        this.protocol = protocol;
        this.srcPort = srcPort;
        this.dstPort = dstPort;
    }
}
