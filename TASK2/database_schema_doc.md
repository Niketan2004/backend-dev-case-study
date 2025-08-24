# Database Schema Design

## Overview
This document outlines the proposed database schema for managing companies, warehouses, products, suppliers, and inventory.  
The schema supports requirements such as multi-warehouse product storage, supplier management, inventory tracking, and product bundles.

### Requirements Given:

* **Companies** can own multiple **warehouses**.
* **Products** are stored in **warehouses**, and their **quantity** can vary per warehouse.
* Changes to **inventory levels** must be logged and tracked.
* **Suppliers** provide **products** to companies.
* Some **products** can be **bundles** of other products..
---

## Schema Explanation

### 1. **Companies**
- Each company can own multiple warehouses, products, and suppliers.
- Primary key: `company_id`.

### 2. **Warehouses**
- Belongs to a company.
- Stores products via the `INVENTORY` table.

### 3. **Products**
- Belongs to a company.
- SKU must be unique across the platform.
- Can be marked as a bundle (`is_bundle`).
- Bundles reference other products via the `PRODUCT_BUNDLES` table.

### 4. **Inventory**
- Many-to-many relationship between warehouses and products.
- Tracks stock quantity per warehouse.

### 5. **Inventory Log**
- Tracks every change in inventory levels (increases, decreases, transfers, etc.).
- Includes reason for changes.

### 6. **Suppliers**
- Belongs to a company.
- Supplies products to the company.

### 7. **Supplier Products**
- Maps suppliers to the products they provide.
- Includes pricing and lead time information.

### 8. **Product Bundles**
- Represents bundle-to-component relationships.
- A bundle product can include multiple component products with specified quantities.

---

## Design Decisions

1. **Indexes & Constraints**
   - Unique index on `Products.sku` for global uniqueness.
   - Foreign keys to maintain referential integrity (e.g., `warehouse_id`, `company_id`, `product_id`).

2. **Auditability**
   - `Inventory_Log` provides a full audit trail for stock movements.

3. **Flexibility**
   - Bundles handled with a join table so products can be both standalone and part of bundles.

4. **Scalability**
   - Separating `SUPPLIER_PRODUCTS` allows multiple suppliers per product.

---

## Questions for Product Team

1. Should SKUs be unique **globally** or **per company**?  
2. Do we need to track inventory at **lot/batch level** (e.g., expiration date, manufacturing date)?  
3. Should bundles support **nested bundles** (a bundle inside another bundle)?  
4. How should price be determined for bundles — **sum of parts** or a **custom price**?  
5. Do suppliers belong to a company exclusively, or can **multiple companies share suppliers**?  
6. Do suppliers have **minimum order quantities** or **contract prices**?  
7. Can **multiple companies share the same warehouse**, or is it strictly one company per warehouse?  


---
