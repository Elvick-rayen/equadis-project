-- The three persona types a caller can act as (see the X-Acting-As header).
-- Reference data: present in every environment, so it lives in the migration, not the seed.
CREATE TABLE persona (
    code        VARCHAR(20)  PRIMARY KEY,
    description VARCHAR(200) NOT NULL
);

INSERT INTO persona (code, description) VALUES
    ('ADMIN',    'Administrator: manages suppliers, retailers and partnerships'),
    ('SUPPLIER', 'Supplier: manages its commercial offers'),
    ('RETAILER', 'Retailer: browses and subscribes to offers');
