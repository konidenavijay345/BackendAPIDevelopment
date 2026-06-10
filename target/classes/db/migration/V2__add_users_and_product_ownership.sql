CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(60) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

ALTER TABLE products
    ADD COLUMN owner_id BIGINT NULL AFTER id,
    ADD CONSTRAINT fk_products_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE products DROP INDEX uk_products_sku;
ALTER TABLE products ADD CONSTRAINT uk_products_owner_sku UNIQUE (owner_id, sku);
CREATE INDEX idx_products_owner_id ON products (owner_id);
