-- Audit entries now name companies instead of showing their ids. Rewrite the entries recorded
-- before that change: "admin (as SUPPLIER:3)" -> "admin (as Alpina Foods)" and
-- "Supplier #3, retailer #1, ..." -> "Supplier Alpina Foods, retailer City Market Lyon, ...".

UPDATE audit_entry a
SET actor = replace(a.actor, '(as SUPPLIER:' || s.id || ')', '(as ' || s.company_name || ')')
FROM supplier s
WHERE a.actor LIKE '%(as SUPPLIER:' || s.id || ')';

UPDATE audit_entry a
SET actor = replace(a.actor, '(as RETAILER:' || r.id || ')', '(as ' || r.store_name || ')')
FROM retailer r
WHERE a.actor LIKE '%(as RETAILER:' || r.id || ')';

UPDATE audit_entry a
SET details = replace(a.details, 'Supplier #' || s.id || ',', 'Supplier ' || s.company_name || ',')
FROM supplier s
WHERE a.details LIKE 'Supplier #' || s.id || ',%';

UPDATE audit_entry a
SET details = replace(a.details, 'retailer #' || r.id || ',', 'retailer ' || r.store_name || ',')
FROM retailer r
WHERE a.details LIKE '%, retailer #' || r.id || ',%';
