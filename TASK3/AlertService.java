import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AlertService {

     @Autowired
     private ProductRepository productRepo;

     @Autowired
     private InventoryRepository inventoryRepo;

     @Autowired
     private SalesRepository salesRepo;

     @Autowired
     private SupplierRepository supplierRepo;

     @Autowired
     private WarehouseRepository warehouseRepo;

     /**
      * Core logic: Generate low-stock alerts for all products of a company.
      * 
      * Steps:
      * 1. Fetch all products for the given company.
      * 2. For each product, check if it had recent sales (only then it's relevant).
      * 3. Check inventory across all warehouses for that product.
      * 4. If stock is below threshold → prepare alert data.
      * 5. Enrich alert with warehouse + supplier information.
      * 6. Estimate stockout days (simple daily avg sales assumption).
      */
     public List<LowStockAlertDTO> getLowStockAlerts(Long companyId) {
          List<LowStockAlertDTO> alerts = new ArrayList<>();

          // 1. Get all products that belong to this company
          List<Product> products = productRepo.findByCompanyId(companyId);

          for (Product product : products) {
               int threshold = product.getThreshold(); // low-stock threshold for this product

               // 2. Check if product had sales in the last 30 days
               boolean recentSales = salesRepo.existsByProductIdAndDateAfter(
                         product.getId(),
                         LocalDate.now().minusDays(30));

               if (!recentSales) {
                    // Skip products without recent sales → avoids spamming alerts for inactive
                    // items
                    continue;
               }

               // 3. Get inventory for this product across all warehouses
               List<Inventory> inventories = inventoryRepo.findByProductId(product.getId());

               for (Inventory inv : inventories) {
                    // 4. If current stock < threshold → low stock situation
                    if (inv.getQuantity() < threshold) {

                         // 5. Get warehouse info (to know where stock is low)
                         Warehouse warehouse = warehouseRepo.findById(inv.getWarehouseId()).orElse(null);

                         // 6. Get supplier info (so company knows whom to reorder from)
                         Supplier supplier = supplierRepo.findById(product.getSupplierId()).orElse(null);

                         // 7. Estimate days until stockout
                         // Assumption: use avg daily sales (over last 30 days).
                         // If no sales data → mark as -1 (unknown).
                         int dailySalesRate = salesRepo.getAverageDailySales(product.getId(), 30);
                         int daysUntilStockout = dailySalesRate > 0 ? inv.getQuantity() / dailySalesRate : -1;

                         // 8. Build DTO with all alert info
                         LowStockAlertDTO dto = new LowStockAlertDTO(
                                   product.getId(),
                                   product.getName(),
                                   product.getSku(),
                                   inv.getWarehouseId(),
                                   warehouse != null ? warehouse.getName() : "Unknown",
                                   inv.getQuantity(),
                                   threshold,
                                   daysUntilStockout,
                                   supplier // directly embedding supplier object in DTO
                         );

                         alerts.add(dto); // add this alert to the list
                    }
               }
          }

          // 9. Return final list of alerts
          return alerts;
     }
}
