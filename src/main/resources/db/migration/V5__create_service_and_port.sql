CREATE TABLE port (
                      id       BIGINT PRIMARY KEY,
                      protocol VARCHAR(20) NOT NULL,   -- TCP, UDP, ICMP и т.д.
                      port     INTEGER NOT NULL,

                      CONSTRAINT uq_port_protocol_port UNIQUE (protocol, port)
);

COMMENT ON TABLE port IS 'Порты и протоколы (80/TCP, 443/TCP, 53/UDP и т.п.)';

CREATE TABLE service (
                         id          BIGSERIAL PRIMARY KEY,
                         name        VARCHAR(100) NOT NULL UNIQUE,
                         description VARCHAR(500)
);

COMMENT ON TABLE service IS 'Сервисы / группы портов (HTTP, HTTPS, DNS, SSH и т.п.)';

-- ManyToMany связь Service ↔ Port
CREATE TABLE service_ports (
                               service_id BIGINT NOT NULL REFERENCES service(id) ON DELETE CASCADE,
                               port_id    BIGINT NOT NULL REFERENCES port(id)    ON DELETE RESTRICT,

                               PRIMARY KEY (service_id, port_id)
);

CREATE INDEX idx_service_ports_service ON service_ports(service_id);
CREATE INDEX idx_service_ports_port    ON service_ports(port_id);