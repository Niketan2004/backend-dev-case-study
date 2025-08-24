-- Companies
CREATE TABLE companies (
    company_id      BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name            VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP DEFAULT NOW()
);

-- Warehouses
CREATE TABLE warehouses (
    warehouse_id    BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    company_id      BIGINT NOT NULL REFERENCES companies(company_id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    location        VARCHAR(255),
    created_at      TIMESTAMP DEFAULT NOW()
);

-- Products
CREATE TABLE products (
    product_id      BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    company_id      BIGINT NOT NULL REFERENCES companies(company_id) ON DELETE CASCADE,
    sku             VARCHAR(100) UNIQUE NOT NULL,
    name            VARCHAR(255) NOT NULL,
    price           DECIMAL(12,2) NOT NULL,
    is_bundle       BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP DEFAULT NOW()
);

-- Inventory: many-to-many between warehouses and products
CREATE TABLE inventory (
    inventory_id    BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    product_id      BIGINT NOT NULL REFERENCES products(product_id) ON DELETE CASCADE,
    warehouse_id    BIGINT NOT NULL REFERENCES warehouses(warehouse_id) ON DELETE CASCADE,
    quantity        INT NOT NULL DEFAULT 0,
    updated_at      TIMESTAMP DEFAULT NOW(),
    UNIQUE (product_id, warehouse_id)
);

-- Inventory change history (audit log)
CREATE TABLE inventory_log (
    log_id          BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    product_id      BIGINT NOT NULL REFERENCES products(product_id),
    warehouse_id    BIGINT NOT NULL REFERENCES warehouses(warehouse_id),
    change_amount   INT NOT NULL,       -- +10, -5, etc.
    reason          VARCHAR(255),       -- e.g., purchase, sale, adjustment
    created_at      TIMESTAMP DEFAULT NOW()
);

-- Suppliers
CREATE TABLE suppliers (
    supplier_id     BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    company_id      BIGINT NOT NULL REFERENCES companies(company_id),
    name            VARCHAR(255) NOT NULL,
    contact_info    JSONB,              -- flexible for phone/email/etc.
    created_at      TIMESTAMP DEFAULT NOW()
);

-- Supplier → Product relationship
CREATE TABLE supplier_products (
    supplier_id     BIGINT NOT NULL REFERENCES suppliers(supplier_id) ON DELETE CASCADE,
    product_id      BIGINT NOT NULL REFERENCES products(product_id) ON DELETE CASCADE,
    lead_time_days  INT,
    price           DECIMAL(12,2),     -- optional supplier-specific price
    PRIMARY KEY (supplier_id, product_id)
);

-- Bundles: self-referencing many-to-many for products
CREATE TABLE product_bundles (
    bundle_id       BIGINT NOT NULL REFERENCES products(product_id) ON DELETE CASCADE,
    component_id    BIGINT NOT NULL REFERENCES products(product_id) ON DELETE CASCADE,
    quantity        INT NOT NULL DEFAULT 1, -- e.g., 2 pens in a stationery kit
    PRIMARY KEY (bundle_id, component_id),
    CHECK (bundle_id <> component_id)       -- prevent self-reference
);
