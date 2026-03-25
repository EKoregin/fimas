-- =============================================
-- V2__create_action_command_and_update_model.sql
-- Добавляем таблицы Action, Command и связь Model <-> Action
-- =============================================

-- 1. Таблица Command
CREATE TABLE command (
                         id          BIGSERIAL PRIMARY KEY,
                         name        VARCHAR(100) NOT NULL,
                         command     TEXT         NOT NULL,           -- может быть длинным (API path или CLI)
                         command_type VARCHAR(20) NOT NULL,            -- SSH или HTTPS
                         vendor_id   BIGINT       NOT NULL,

                         CONSTRAINT fk_command_vendor FOREIGN KEY (vendor_id) REFERENCES vendor(id) ON DELETE RESTRICT,
                         CONSTRAINT chk_command_type CHECK (command_type IN ('SSH', 'HTTPS'))
);

CREATE INDEX idx_command_vendor ON command(vendor_id);
CREATE INDEX idx_command_type ON command(command_type);

-- 2. Таблица Action
CREATE TABLE action (
                        id   BIGSERIAL PRIMARY KEY,
                        name VARCHAR(100) NOT NULL
);

-- 3. Связь Action <-> Command (с сохранением порядка выполнения)
CREATE TABLE action_command (
                                action_id     BIGINT NOT NULL,
                                command_id    BIGINT NOT NULL,
                                command_order INT    NOT NULL DEFAULT 0,     -- порядок выполнения команд

                                PRIMARY KEY (action_id, command_id),
                                CONSTRAINT fk_action_command_action FOREIGN KEY (action_id) REFERENCES action(id) ON DELETE CASCADE,
                                CONSTRAINT fk_action_command_command FOREIGN KEY (command_id) REFERENCES command(id) ON DELETE CASCADE
);

CREATE INDEX idx_action_command_action ON action_command(action_id);
CREATE INDEX idx_action_command_order ON action_command(action_id, command_order);

-- 4. Связь Model <-> Action (ManyToMany)
CREATE TABLE model_action (
                              model_id  BIGINT NOT NULL,
                              action_id BIGINT NOT NULL,

                              PRIMARY KEY (model_id, action_id),
                              CONSTRAINT fk_model_action_model FOREIGN KEY (model_id) REFERENCES model(id) ON DELETE CASCADE,
                              CONSTRAINT fk_model_action_action FOREIGN KEY (action_id) REFERENCES action(id) ON DELETE CASCADE
);

CREATE INDEX idx_model_action_model ON model_action(model_id);
CREATE INDEX idx_model_action_action ON model_action(action_id);

-- =============================================
-- Если в таблице model уже есть данные, можно добавить комментарий
COMMENT ON TABLE command IS 'Команды, которые могут выполняться на firewall';
COMMENT ON TABLE action IS 'Действия (политики), которые можно выполнить на модели';
COMMENT ON COLUMN command.command IS 'Текст команды (CLI) или путь API';