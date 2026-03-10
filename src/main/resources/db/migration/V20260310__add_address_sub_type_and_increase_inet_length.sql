-- 1. Увеличиваем длину колонки inet_address (безопасная операция)
ALTER TABLE inet_addresses
    ALTER COLUMN inet_address TYPE VARCHAR(512);

-- 2. Добавляем новую колонку sub_type в основную таблицу address
-- Используем VARCHAR(10) вместо настоящего enum-типа, чтобы миграция была проще и переносима
ALTER TABLE address
    ADD COLUMN sub_type VARCHAR(10) NULL;   -- пока NULL, чтобы не ломать существующие записи

-- 3. (Опционально) Заполняем sub_type для существующих записей
-- Здесь предполагаем, что пока все старые записи считаем IP
-- Если у вас есть логика определения типа — можно написать более умный UPDATE
UPDATE address
SET sub_type = 'IP'
WHERE sub_type IS NULL;

-- 4. Делаем колонку NOT NULL после заполнения
ALTER TABLE address
    ALTER COLUMN sub_type SET NOT NULL;

-- 5. (Опционально) Добавляем CHECK-констрейнт для защиты от неверных значений на уровне БД
ALTER TABLE address
    ADD CONSTRAINT chk_address_sub_type
        CHECK (sub_type IN ('IP', 'FQDN'));

-- 6. (Опционально) Индекс, если часто фильтруем по типу
CREATE INDEX idx_address_sub_type ON address (sub_type);