CREATE TABLE firewall (
                          id          BIGSERIAL PRIMARY KEY,
                          name        VARCHAR(100) NOT NULL,
                          description VARCHAR(500),
                          model_id    BIGINT NOT NULL,

                          CONSTRAINT fk_firewall_model
                              FOREIGN KEY (model_id) REFERENCES model(id)
                                  ON DELETE RESTRICT
);

CREATE INDEX idx_firewall_model_id ON firewall(model_id);

COMMENT ON TABLE firewall IS 'Конкретные экземпляры межсетевых экранов / устройств';