-- V11__populate_common_services_and_ports.sql
-- Заполнение справочников сервисов и портов (идемпотентно)

BEGIN;

-- =====================================================================
-- 1. Очистка старых записей (раскомментировать при необходимости)
-- =====================================================================
-- DELETE FROM service_ports;
-- DELETE FROM port WHERE dest_port IS NOT NULL;
-- DELETE FROM service WHERE name ~ '^[0-9]' OR name LIKE '%-%';

-- =====================================================================
-- 2. Добавляем уникальные порты (только dest_port, src_port остаётся NULL)
-- =====================================================================

INSERT INTO port (protocol, dest_port)
VALUES
    -- Только TCP
    ('TCP', '179'), ('TCP', '20'),  ('TCP', '21'),  ('TCP', '22'),  ('TCP', '23'),
    ('TCP', '49'),  ('TCP', '80'),  ('TCP', '99'),  ('TCP', '1123'),
    ('TCP', '1742'), ('TCP', '2379'), ('TCP', '2380'), ('TCP', '3022'),
    ('TCP', '3023'), ('TCP', '3389'), ('TCP', '5900'), ('TCP', '6443'),
    ('TCP', '6568'), ('TCP', '8081'), ('TCP', '8090'), ('TCP', '8095'),
    ('TCP', '8123'), ('TCP', '8443'), ('TCP', '9000'), ('TCP', '9200'),
    ('TCP', '9201'), ('TCP', '12123'), ('TCP', '10050'), ('TCP', '10051'),
    ('TCP', '10257'), ('TCP', '10259'), ('TCP', '10275'), ('TCP', '21114-21119'),

    -- Только UDP
    ('UDP', '623'), ('UDP', '664'), ('UDP', '1701'), ('UDP', '3784'),
    ('UDP', '4500'), ('UDP', '50001-50003'),

    -- Оба протокола (TCP + UDP) — добавляем две записи
    ('TCP', '53'),  ('UDP', '53'),
    ('TCP', '123'), ('UDP', '123'),
    ('TCP', '161'), ('UDP', '161'),
    ('TCP', '162'), ('UDP', '162'),
    ('TCP', '443'), ('UDP', '443'),
    ('TCP', '500'), ('UDP', '500'),
    ('TCP', '853'), ('UDP', '853'),
    ('TCP', '8211'), ('UDP', '8211'),
    ('TCP', '8883'), ('UDP', '8883'),
    ('TCP', '9091'), ('UDP', '9091'),
    ('TCP', '9092'), ('UDP', '9092'),
    ('TCP', '9093'), ('UDP', '9093'),
    ('TCP', '9098'), ('UDP', '9098'),
    ('TCP', '25001'), ('UDP', '25001'),
    ('TCP', '13000'), ('UDP', '13000'),
    ('TCP', '14000'), ('UDP', '14000'),
    ('TCP', '15000'), ('UDP', '15000'),
    ('TCP', '0-21'),  ('UDP', '0-21'),
    ('TCP', '23-3388'),('UDP', '23-3388'),
    ('TCP', '3390-65535'),('UDP', '3390-65535'),

    -- TCP/UDP сервисы с диапазонами и особыми случаями
    ('TCP', '1883'), ('TCP', '4343'), ('TCP', '4505'), ('TCP', '4506'),
    ('TCP', '5432'), ('UDP', '1883'), ('UDP', '4343'), ('UDP', '4505'),
    ('UDP', '4506'), ('UDP', '5432')
ON CONFLICT DO NOTHING;

-- =====================================================================
-- 3. Заполняем таблицу service
-- =====================================================================

INSERT INTO service (name, description)
VALUES
    ('tcp-179',        'BGP'),
    ('tcp-20',         'FTP data'),
    ('tcp-21',         'FTP control'),
    ('tcp-22',         'SSH'),
    ('tcp-23',         'Telnet'),
    ('tcp-49',         'TACACS+'),
    ('53',             'DNS'),
    ('tcp-80',         'HTTP'),
    ('tcp-99',         'WSN'),
    ('123',            'NTP'),
    ('161',            'SNMP'),
    ('162',            'SNMPTRAP'),
    ('443',            'HTTPS'),
    ('500',            'ISAKMP / IKE'),
    ('udp-623',        'ASF RMCP (IPMI)'),
    ('udp-664',        'OpenFlow'),
    ('853',            'DNS over TLS'),
    ('tcp-1123',       'Custom'),
    ('udp-1701',       'L2TP'),
    ('1742',           'Custom'),
    ('1883',           'MQTT'),
    ('tcp-2379',       'etcd client'),
    ('tcp-2380',       'etcd peer'),
    ('tcp-3022',       'TCP proxy'),
    ('tcp-3023',       'TCP proxy'),
    ('tcp-3389',       'RDP'),
    ('udp-3784',       'BFD Echo'),
    ('4343',           'UNICAST-MSDS'),
    ('udp-4500',       'IPsec NAT-T'),
    ('4505',           'Salt master'),
    ('4506',           'Salt minion'),
    ('5432',           'PostgreSQL'),
    ('tcp-5900',       'VNC'),
    ('tcp-6443',       'Kubernetes API'),
    ('tcp-6568',       'Custom'),
    ('8000',           'HTTP alternate'),
    ('tcp-8081',       'HTTP proxy'),
    ('tcp-8090',       'Web interface'),
    ('tcp-8095',       'Custom'),
    ('tcp-8123',       'Custom'),
    ('8211',           'Custom'),
    ('tcp-8443',       'HTTPS alternate'),
    ('8883',           'MQTT TLS'),
    ('tcp-9000',       'Prometheus'),
    ('9091',           'Alertmanager'),
    ('9092',           'Kafka'),
    ('9093',           'Alertmanager'),
    ('9098',           'Custom'),
    ('tcp-9200',       'Elasticsearch'),
    ('tcp-9201',       'Elasticsearch alt'),
    ('tcp-12123',      'Custom'),
    ('tcp-10050',      'Zabbix agent'),
    ('tcp-10051',      'Zabbix server'),
    ('tcp-10257',      'kube-controller-manager'),
    ('tcp-10259',      'kube-scheduler'),
    ('tcp-10275',      'Custom'),
    ('13000',          'Custom range'),
    ('14000',          'Custom range'),
    ('15000',          'Custom range'),
    ('udp-21116',      'Custom'),
    ('tcp-21114-21119','Range TCP'),
    ('21114-21119',    'Range TCP/UDP'),
    ('25001',          'Custom'),
    ('30000-32500',    'Kubernetes NodePort'),
    ('0-21',           'Low ports'),
    ('23-3388',        'Privileged ports'),
    ('3390-65535',     'High ports'),
    ('udp-50001-50003','UDP range')
ON CONFLICT (name) DO NOTHING;

-- =====================================================================
-- 4. Связываем сервисы с портами (более надёжная логика)
-- =====================================================================

INSERT INTO service_ports (service_id, port_id)
SELECT DISTINCT s.id, p.id
FROM service s
         JOIN port p ON
    -- Извлекаем номер порта из имени сервиса
    p.dest_port =
    CASE
        WHEN s.name ~ '^[0-9]+(-[0-9]+)?$' THEN s.name
        WHEN s.name ~ '^tcp-' THEN REGEXP_REPLACE(s.name, '^tcp-', '', 'g')
        WHEN s.name ~ '^udp-' THEN REGEXP_REPLACE(s.name, '^udp-', '', 'g')
        ELSE NULL
        END
WHERE
   -- TCP-сервисы → только TCP-порты
    (s.name LIKE 'tcp-%' AND p.protocol = 'TCP')
   OR
   -- UDP-сервисы → только UDP-порты
    (s.name LIKE 'udp-%' AND p.protocol = 'UDP')
   OR
   -- Сервисы без префикса (53, 443, 123 и т.д.) → оба протокола
    (NOT s.name LIKE '%-%' AND p.protocol IN ('TCP', 'UDP'))
   OR
   -- Специальные сервисы с диапазонами / TCP/UDP в названии
    (s.name IN ('1883','4343','4505','4506','5432','21114-21119')
        AND p.dest_port = s.name)
ON CONFLICT (service_id, port_id) DO NOTHING;

COMMIT;