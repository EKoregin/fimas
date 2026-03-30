-- V20260330_002__insert_default_zones.sql

-- Вставка стандартных зон (idempotentно)
INSERT INTO zone (name, description, priority)
VALUES
    ('LAN', 'Local Area Network - внутренняя сеть', 10),
    ('WAN', 'Wide Area Network - внешняя сеть / интернет', 20),
    ('DC',  'Data Center - дата-центр', 15)
ON CONFLICT (name) DO NOTHING;

-- Пример интерфейсов (можно расширить позже)
INSERT INTO zone_interfaces (zone_id, interface_name)
SELECT z.id, i.interface_name
FROM zone z
         CROSS JOIN (VALUES
                         ('LAN', 'any'), ('LAN', 'any'),
                         ('WAN', 'any'), ('WAN', 'any'),
                         ('DC',  'any')
) AS i(zone_name, interface_name)
WHERE z.name = i.zone_name
ON CONFLICT (zone_id, interface_name) DO NOTHING;