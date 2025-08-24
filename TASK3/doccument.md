#  Low Stock Alert API – Spring Boot Project

##  Project Overview

This project implements a REST API endpoint that generates **low stock alerts** for products in a company.

It ensures businesses can identify which products are at risk of stockout and quickly reorder them from suppliers.

The system considers **thresholds, recent sales, multiple warehouses, and supplier details**.

---

##  Business Rules & Assumptions

* **Low stock threshold** varies per product (`Product.threshold`).
* Only **products with recent sales activity (last 30 days)** are considered relevant.
* Companies may have **multiple warehouses**, so stock is checked across each warehouse.
* Alerts must include **supplier information** so that reordering is easier.
* **Days until stockout** is estimated using **average daily sales in the last 30 days**.

  * If no sales data exists → set `-1` (unknown).

---

##  API Specification

**Endpoint:**

```
GET /api/companies/{companyId}/alerts/low-stock
```

**Response Example:**

```json
{
  "alerts": [
    {
      "product_id": 123,
      "product_name": "Widget A",
      "sku": "WID-001",
      "warehouse_id": 456,
      "warehouse_name": "Main Warehouse",
      "current_stock": 5,
      "threshold": 20,
      "days_until_stockout": 12,
      "supplier": {
        "id": 789,
        "name": "Supplier Corp",
        "contact_email": "orders@supplier.com"
      }
    }
  ],
  "total_alerts": 1
}
```

---

##  Implementation Logic

### **Service: `AlertService`**

```java
public List<LowStockAlertDTO> getLowStockAlerts(Long companyId)
```

###  Step-by-Step Explanation

1. **Fetch Products**

   * Retrieve all products for the given company.
   * Each product contains its **low-stock threshold**.

2. **Check Sales Activity**

   * Use `salesRepo.existsByProductIdAndDateAfter(productId, LocalDate.now().minusDays(30))`.
   * If no sales in last 30 days → skip the product (not actively sold).

3. **Fetch Inventories**

   * Get inventory records for each product across **all warehouses**.
   * Each inventory record contains **warehouseId** and **current quantity**.

4. **Compare with Threshold**

   * If `currentStock < threshold` → this product qualifies for low stock alert.

5. **Fetch Warehouse Info**

   * Use `warehouseRepo.findById(warehouseId)` to identify which warehouse is low on stock.

6. **Fetch Supplier Info**

   * Use `supplierRepo.findById(product.getSupplierId())` to include supplier contact.

7. **Estimate Days Until Stockout**

   * Calculate `avgDailySales = salesRepo.getAverageDailySales(productId, 30)`.
   * If sales exist → `daysUntilStockout = currentStock / avgDailySales`.
   * If no sales data → return `-1` (unknown).

8. **Build Alert DTO**

   * Create `LowStockAlertDTO` object with all collected data:

     * Product details (ID, Name, SKU)
     * Warehouse details (ID, Name)
     * Current stock & threshold
     * Estimated stockout days
     * Supplier details

9. **Return Alerts**

   * Add each alert to a list.
   * Final result is returned to the Controller.

---

##  Edge Case Handling

* **No Products:** Returns empty alert list.
* **No Sales Data:** Skips products not sold in last 30 days.
* **Warehouse Missing:** Marks warehouse as `"Unknown"`.
* **Supplier Missing:** Returns `null` in supplier field.
* **Zero Sales Rate:** Days until stockout = `-1` (unknown).

---

##  Example Usage

Request:

```
GET /api/companies/101/alerts/low-stock
```

Response:

```json
{
  "alerts": [
    {
      "product_id": 45,
      "product_name": "Laptop Bag",
      "sku": "LBAG-02",
      "warehouse_id": 12,
      "warehouse_name": "Central Warehouse",
      "current_stock": 3,
      "threshold": 15,
      "days_until_stockout": 6,
      "supplier": {
        "id": 7,
        "name": "Global Supplies Ltd.",
        "contact_email": "supply@global.com"
      }
    }
  ],
  "total_alerts": 1
}
```

---

##  Summary

This project demonstrates how to:
 - Use **Spring Boot + Repositories** for business logic.
 - Enforce **business rules** in backend API.
 - Handle **edge cases gracefully**.
 - Return **structured JSON response** for frontend integration.

---
