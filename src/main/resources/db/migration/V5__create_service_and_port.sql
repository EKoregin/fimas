CREATE TABLE port (
                      id       BIGSERIAL PRIMARY KEY,
                      protocol VARCHAR(20) NOT NULL,   -- TCP, UDP, ICMP и т.д.
                      source_port VARCHAR(12) NULL ,
                      dest_port VARCHAR(12) NULL

);

-- Опционально: индексы, если часто будете искать по портам
CREATE INDEX idx_port_source_port ON port(source_port);
CREATE INDEX idx_port_dest_port   ON port(dest_port);

COMMENT ON COLUMN port.source_port IS 'Исходный порт или диапазон (например: 1024-65535, 80)';
COMMENT ON COLUMN port.dest_port   IS 'Целевой порт или диапазон (например: 443, 8080-8090)';

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