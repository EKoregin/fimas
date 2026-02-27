CREATE TABLE model (
                       id         BIGSERIAL PRIMARY KEY,
                       name       VARCHAR(100) NOT NULL,
                       vendor_id  BIGINT NOT NULL,

                       CONSTRAINT fk_model_vendor
                           FOREIGN KEY (vendor_id) REFERENCES vendor(id)
                               ON DELETE RESTRICT
);

CREATE INDEX idx_model_vendor_id ON model(vendor_id);

COMMENT ON TABLE model IS 'Модели оборудования (например ASR1001, SRX1500, PA-5220)';