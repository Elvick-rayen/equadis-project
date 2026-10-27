-- Demo retailers (dev profile only).
INSERT INTO retailer (store_name, store_type, country_code, city, address, website, contact_email) VALUES
    ('City Market Lyon', 'PHYSICAL', 'FR', 'Lyon',      '12 Rue de la République', NULL,                      'contact@citymarket-lyon.fr'),
    ('Bio&Co Paris',     'HYBRID',   'FR', 'Paris',     '48 Boulevard Voltaire',   'https://bioco.fr',        'hello@bioco.fr'),
    ('Hyper Sud',        'PHYSICAL', 'FR', 'Marseille', '5 Avenue du Prado',       NULL,                      'achats@hypersud.fr'),
    ('Kiosk Express',    'ONLINE',   'BE', NULL,        NULL,                      'https://kioskexpress.be', 'support@kioskexpress.be');
