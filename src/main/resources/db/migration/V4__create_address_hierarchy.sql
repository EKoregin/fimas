-- Базовая таблица Address (Joined inheritance)
CREATE TABLE address (
                         id            BIGSERIAL PRIMARY KEY,
                         address_type  VARCHAR(31) NOT NULL,          -- дискриминатор
                         name          VARCHAR(100) NOT NULL,
                         description   VARCHAR(500)
);

-- CommonAddress — пустая таблица (нет доп. полей)
CREATE TABLE common_address (
                                id BIGINT PRIMARY KEY REFERENCES address(id) ON DELETE CASCADE
);

-- DynamicAddress
CREATE TABLE dynamic_address (
                                 id          BIGINT PRIMARY KEY REFERENCES address(id) ON DELETE CASCADE,
                                 firewall_id BIGINT NOT NULL,

                                 CONSTRAINT fk_dynamic_address_firewall
                                     FOREIGN KEY (firewall_id) REFERENCES firewall(id)
                                         ON DELETE RESTRICT
);

CREATE INDEX idx_dynamic_address_firewall_id ON dynamic_address(firewall_id);

-- Коллекция IP-адресов (ElementCollection)
CREATE TABLE inet_addresses (
                                address_id    BIGINT NOT NULL REFERENCES address(id) ON DELETE CASCADE,
                                inet_address  VARCHAR(45) NOT NULL,

                                PRIMARY KEY (address_id, inet_address)
);

CREATE INDEX idx_inet_addresses_address_id ON inet_addresses(address_id);
CREATE INDEX idx_inet_addresses_inet      ON inet_addresses(inet_address);

COMMENT ON TABLE address          IS 'Базовая таблица адресов (абстрактная)';
COMMENT ON TABLE common_address   IS 'Обычные / статические адреса';
COMMENT ON TABLE dynamic_address  IS 'Динамические адреса, привязанные к конкретному firewall';
COMMENT ON TABLE inet_addresses   IS 'IP-адреса и подсети, связанные с любым адресом';