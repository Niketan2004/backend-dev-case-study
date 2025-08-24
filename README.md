# TASK COMPLETED :

1. **Part 1: Code Review & Debugging**  
   **Summary:** Reviewed and fixed a faulty `POST /api/products` endpoint.  
   **What was done**
   - Added JSON parsing and input validation (required vs optional fields).  
   - Used `Decimal` for monetary values to avoid float precision issues.  
   - Made DB operations atomic (single transaction: `flush()` + `commit()`), rollback on errors.  
   - Handled unique-SKU constraint (catch `IntegrityError` → return `409 Conflict`).  
   - Returned correct HTTP status codes (400, 422, 404, 409, 201) and structured JSON errors.  
   - Added clear inline comments and explanations in the code.

2. **Part 2: Database Design**  
   **Summary:** Designed a normalized schema supporting companies, warehouses, multi-warehouse inventory, suppliers and bundles, plus history logging.  
   **Key tables**
   - `companies`, `warehouses`, `products` (sku, price, threshold, is_bundle, attributes)  
   - `inventory` (product_id, warehouse_id, quantity; unique(product,warehouse))  
   - `inventory_log` (append-only audit: change, before/after, reason, actor)  
   - `suppliers`, `supplier_products` (many-to-many + supplier-specific price/leadtime)  
   - `product_bundles` (bundle -> component mapping)  
   **Artifacts created**
   - SQL DDL (Postgres-style) and a Mermaid ERD.  
   - A document listing open questions for the product team (SKU scope, lot/batch tracking, nested bundles, bundle pricing, supplier ownership, MOQ/contracts, shared warehouses).

3. **Part 3: API Implementation**  
   **Summary:** Implemented `GET /api/companies/{companyId}/alerts/low-stock` in Spring Boot to return low-stock alerts with supplier and warehouse info.  
   **Logic (high level)**
   - Fetch products for company.  
   - Skip products without recent sales (last 30 days).  
   - For each product, check inventory across all warehouses.  
   - If `current_stock < product.threshold`, create an alert entry (include warehouse + supplier).  
   - Estimate `days_until_stockout` using average daily sales over last 30 days (or `-1` if unknown).  
   **Edge cases handled**
   - Missing supplier/warehouse → provide sensible defaults (`null` / `"Unknown"`).  
   - Zero sales rate → `days_until_stockout = -1`.  
   - Graceful handling of missing inventories or DB inconsistencies.

---

## Assumptions (concise)
- `product.threshold` exists and is per-product.  
- `product.supplier_id` links product → supplier.  
- `inventory` stores current stock per (product, warehouse).  
- `sales` table exists to compute recent activity / average sales.  
- "Recent sales" = last 30 days (configurable).  
- SKUs are unique (global by default; can be changed to per-company).  

---
