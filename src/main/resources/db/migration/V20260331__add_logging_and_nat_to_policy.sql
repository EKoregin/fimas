-- Добавляем два новых столбца с default = false
ALTER TABLE policy
    ADD COLUMN is_logging BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN is_nat     BOOLEAN NOT NULL DEFAULT FALSE;

-- Обновляем все существующие записи (на всякий случай, хотя DEFAULT уже должен сработать)
UPDATE policy
SET is_logging = FALSE,
    is_nat = FALSE
WHERE is_logging IS NULL OR is_nat IS NULL;