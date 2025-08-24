@Data
@AllArgsConstructor
public class LowStockAlertDTO {
     private Long product_id;
     private String product_name;
     private String sku;
     private Long warehouse_id;
     private String warehouse_name;
     private int current_stock;
     private int threshold;
     private int days_until_stockout;
     private Supplier supplier;
}
