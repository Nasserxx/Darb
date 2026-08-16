ALTER TABLE mosques
    ADD COLUMN IF NOT EXISTS address_country      varchar(100),
    ADD COLUMN IF NOT EXISTS address_postal_code  varchar(20),
    ADD COLUMN IF NOT EXISTS address_street       varchar(200),
    ADD COLUMN IF NOT EXISTS address_house_number varchar(20),
    ADD COLUMN IF NOT EXISTS address_state        varchar(100);
