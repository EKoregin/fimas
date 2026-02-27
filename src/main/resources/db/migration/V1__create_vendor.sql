CREATE TABLE vendor (
                        id   BIGSERIAL PRIMARY KEY,
                        name VARCHAR(100) NOT NULL UNIQUE
);

COMMENT ON TABLE vendor IS 'Производители оборудования (Cisco, Juniper, Palo Alto и т.п.)';