import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/companies")
public class AlertController {

    @Autowired
    private AlertService alertService;

    /**
     * GET /api/companies/{companyId}/alerts/low-stock
     * Returns low stock alerts for a company across all its warehouses
     */
    @GetMapping("/{companyId}/alerts/low-stock")
    public ResponseEntity<Map<String, Object>> getLowStockAlerts(@PathVariable Long companyId) {
        List<LowStockAlertDTO> alerts = alertService.getLowStockAlerts(companyId);

        Map<String, Object> response = new HashMap<>();
        response.put("alerts", alerts);
        response.put("total_alerts", alerts.size());

        return ResponseEntity.ok(response);
    }
}
