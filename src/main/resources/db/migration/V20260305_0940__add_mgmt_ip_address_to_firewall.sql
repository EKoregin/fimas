-- Добавляем поле для IP-адреса управления (только IPv4)
ALTER TABLE firewall
    ADD COLUMN mgmt_ip_address VARCHAR(15) NULL;
-- размещаем после model_id для логической группировки

-- Устанавливаем комментарий к полю (очень полезно для будущих разработчиков и админов БД)
COMMENT ON COLUMN firewall.mgmt_ip_address
    IS 'IP-адрес для управления устройством (только IPv4, формат: 192.168.1.1)';

-- Добавляем индекс по полю — ускоряет поиск по IP и фильтрацию
-- (особенно полезно, если будет часто выполняться поиск по конкретному firewall по IP)
CREATE INDEX idx_firewall_mgmt_ip_address
    ON firewall (mgmt_ip_address);

CREATE UNIQUE INDEX uix_firewall_mgmt_ip_address
    ON firewall (mgmt_ip_address);