-- Demo data (dev profile only). Dates are relative to CURRENT_DATE, so every status shows up
-- whenever the database is created: ACTIVE, SCHEDULED, EXPIRED (renewable or not), TERMINATED,
-- ending soon, plus ACTIVE, SUSPENDED and CANCELLED subscriptions.
-- Demo accounts: "supplier" is Alpina Foods, "retailer" is City Market Lyon (partners).

CREATE TEMP TABLE seed_company AS
SELECT
    (SELECT id FROM supplier WHERE company_name = 'Maison Lumière')  AS maison,
    (SELECT id FROM supplier WHERE company_name = 'Nordic Pure')     AS nordic,
    (SELECT id FROM supplier WHERE company_name = 'Alpina Foods')    AS alpina,
    (SELECT id FROM supplier WHERE company_name = 'Bella Pasta')     AS bella,
    (SELECT id FROM retailer WHERE store_name = 'City Market Lyon')  AS city_market,
    (SELECT id FROM retailer WHERE store_name = 'Bio&Co Paris')      AS bioco,
    (SELECT id FROM retailer WHERE store_name = 'Hyper Sud')         AS hyper_sud,
    (SELECT id FROM retailer WHERE store_name = 'Kiosk Express')     AS kiosk;

-- Partnerships
INSERT INTO partnership (supplier_id, retailer_id, start_date, end_date, terminated_on, termination_reason)
SELECT maison, city_market, CURRENT_DATE - 60,  CURRENT_DATE + 300, NULL::date, NULL::varchar FROM seed_company -- ACTIVE
UNION ALL SELECT nordic, bioco,       CURRENT_DATE + 30,  CURRENT_DATE + 395, NULL, NULL FROM seed_company -- SCHEDULED
UNION ALL SELECT alpina, hyper_sud,   CURRENT_DATE - 400, CURRENT_DATE - 10,  NULL, NULL FROM seed_company -- EXPIRED, renewable (<= 30 days)
UNION ALL SELECT maison, kiosk,       CURRENT_DATE - 100, CURRENT_DATE + 200, CURRENT_DATE - 20,
                 'Retailer closed its online shop' FROM seed_company                                       -- TERMINATED
UNION ALL SELECT alpina, city_market, CURRENT_DATE - 30,  CURRENT_DATE + 20,  NULL, NULL FROM seed_company -- ACTIVE, ends soon
UNION ALL SELECT nordic, city_market, CURRENT_DATE - 200, CURRENT_DATE + 165, NULL, NULL FROM seed_company -- ACTIVE, renewed once
UNION ALL SELECT bella,  hyper_sud,   CURRENT_DATE - 500, CURRENT_DATE - 60,  NULL, NULL FROM seed_company; -- EXPIRED, too late to renew

-- Renewal history of Nordic Pure ↔ City Market Lyon
INSERT INTO partnership_renewal (partnership_id, previous_end_date, new_end_date, renewed_on)
SELECT p.id, CURRENT_DATE - 20, CURRENT_DATE + 165, CURRENT_DATE - 40
FROM partnership p, seed_company c
WHERE p.supplier_id = c.nordic AND p.retailer_id = c.city_market;

-- Offers
INSERT INTO offer (supplier_id, reference, title, description, discount_type, discount_value, currency,
                   minimum_order_quantity, valid_from, valid_to, status)
SELECT maison, 'ML-SPRING-CANDLES', 'Spring candles', 'Scented candles for the spring collection.',
       'PERCENTAGE_DISCOUNT', 15, NULL::varchar, NULL::integer, CURRENT_DATE - 10, CURRENT_DATE + 60, 'PUBLISHED' FROM seed_company
UNION ALL SELECT maison, 'ML-DISPLAY-BUNDLE', 'Display bundle', 'Shelf display with 24 candles.',
       'FIXED_AMOUNT_DISCOUNT', 250, 'EUR', 5, CURRENT_DATE - 5, CURRENT_DATE + 2, 'PUBLISHED' FROM seed_company
UNION ALL SELECT nordic, 'NP-WATER-6PACK', 'Water 6-pack', NULL,
       'PERCENTAGE_DISCOUNT', 8, NULL, NULL, CURRENT_DATE - 15, CURRENT_DATE + 80, 'PUBLISHED' FROM seed_company
UNION ALL SELECT alpina, 'ALP-WINTER-01', 'Winter cereal promotion', 'Draft for the winter season.',
       'PERCENTAGE_DISCOUNT', 12, NULL, NULL, CURRENT_DATE + 30, CURRENT_DATE + 60, 'DRAFT' FROM seed_company
UNION ALL SELECT alpina, 'ALP-MUESLI-BULK', 'Muesli bulk order', 'For orders of at least 20 cases.',
       'FIXED_AMOUNT_DISCOUNT', 50, 'EUR', 20, CURRENT_DATE - 20, CURRENT_DATE + 90, 'PUBLISHED' FROM seed_company
UNION ALL SELECT alpina, 'ALP-SUMMER-25', 'Summer granola', NULL,
       'PERCENTAGE_DISCOUNT', 10, NULL, NULL, CURRENT_DATE - 120, CURRENT_DATE - 30, 'CLOSED' FROM seed_company
UNION ALL SELECT bella, 'BP-FUSILLI', 'Fusilli family pack', NULL,
       'PERCENTAGE_DISCOUNT', 20, NULL, NULL, CURRENT_DATE - 5, CURRENT_DATE + 45, 'PUBLISHED' FROM seed_company;

-- Subscriptions
INSERT INTO subscription (retailer_id, offer_id, supplier_id, subscribed_on, cancelled_on, cancellation_cause)
SELECT c.city_market, o.id, o.supplier_id, CURRENT_DATE - 5, NULL::date, NULL::varchar
FROM offer o, seed_company c WHERE o.reference = 'ML-SPRING-CANDLES'                        -- ACTIVE
UNION ALL
SELECT c.city_market, o.id, o.supplier_id, CURRENT_DATE - 10, NULL, NULL
FROM offer o, seed_company c WHERE o.reference = 'ALP-MUESLI-BULK'                          -- ACTIVE
UNION ALL
SELECT c.hyper_sud, o.id, o.supplier_id, CURRENT_DATE - 30, NULL, NULL
FROM offer o, seed_company c WHERE o.reference = 'ALP-MUESLI-BULK'                          -- SUSPENDED (partnership expired)
UNION ALL
SELECT c.city_market, o.id, o.supplier_id, CURRENT_DATE - 90, CURRENT_DATE - 30, 'OFFER_CLOSED'
FROM offer o, seed_company c WHERE o.reference = 'ALP-SUMMER-25'                            -- CANCELLED
UNION ALL
SELECT c.kiosk, o.id, o.supplier_id, CURRENT_DATE - 40, CURRENT_DATE - 20, 'PARTNERSHIP_TERMINATED'
FROM offer o, seed_company c WHERE o.reference = 'ML-SPRING-CANDLES';                       -- CANCELLED

DROP TABLE seed_company;
