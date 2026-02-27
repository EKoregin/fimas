CREATE TABLE policy (
                        id          BIGSERIAL PRIMARY KEY,
                        name        VARCHAR(100) NOT NULL,
                        description VARCHAR(500),
                        action      VARCHAR(50)  NOT NULL,     -- ALLOW, DENY, REJECT, ...
                        status      VARCHAR(50)  NOT NULL,     -- ACTIVE, INACTIVE, DRAFT, ...
                        firewall_id BIGINT NOT NULL,

                        CONSTRAINT fk_policy_firewall
                            FOREIGN KEY (firewall_id) REFERENCES firewall(id)
                                ON DELETE RESTRICT
);

CREATE INDEX idx_policy_firewall_id ON policy(firewall_id);

-- ManyToMany: Policy → Source Addresses
CREATE TABLE policy_src_addresses (
                                      policy_id  BIGINT NOT NULL REFERENCES policy(id)  ON DELETE CASCADE,
                                      address_id BIGINT NOT NULL REFERENCES address(id) ON DELETE RESTRICT,

                                      PRIMARY KEY (policy_id, address_id)
);

-- ManyToMany: Policy → Destination Addresses
CREATE TABLE policy_dst_addresses (
                                      policy_id  BIGINT NOT NULL REFERENCES policy(id)  ON DELETE CASCADE,
                                      address_id BIGINT NOT NULL REFERENCES address(id) ON DELETE RESTRICT,

                                      PRIMARY KEY (policy_id, address_id)
);

-- ManyToMany: Policy → Services
CREATE TABLE policy_services (
                                 policy_id  BIGINT NOT NULL REFERENCES policy(id)   ON DELETE CASCADE,
                                 service_id BIGINT NOT NULL REFERENCES service(id)  ON DELETE RESTRICT,

                                 PRIMARY KEY (policy_id, service_id)
);

-- Индексы для ускорения типичных запросов
CREATE INDEX idx_policy_src_addresses_policy  ON policy_src_addresses(policy_id);
CREATE INDEX idx_policy_dst_addresses_policy  ON policy_dst_addresses(policy_id);
CREATE INDEX idx_policy_services_policy       ON policy_services(policy_id);