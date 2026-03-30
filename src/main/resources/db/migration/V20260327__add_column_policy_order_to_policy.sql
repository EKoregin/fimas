ALTER TABLE policy ADD COLUMN policy_order INTEGER;
UPDATE policy SET policy_order = id;