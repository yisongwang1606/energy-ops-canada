package ca.yisong.energyops.service;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import ca.yisong.energyops.api.ApiModels.MaintenanceRecordRequest;
import ca.yisong.energyops.api.ApiModels.SiteRequest;
import ca.yisong.energyops.api.ApiModels.WorkOrderCreateRequest;
import ca.yisong.energyops.api.ApiModels.WorkOrderUpdateRequest;
import ca.yisong.energyops.model.Alert;
import ca.yisong.energyops.model.UserAccount;
import ca.yisong.energyops.model.UserRole;
import ca.yisong.energyops.repository.AlertRepository;
import ca.yisong.energyops.repository.AssetRepository;
import ca.yisong.energyops.repository.MaintenanceRecordRepository;
import ca.yisong.energyops.repository.SensorReadingRepository;
import ca.yisong.energyops.repository.SiteRepository;
import ca.yisong.energyops.repository.UserAccountRepository;
import ca.yisong.energyops.repository.WorkOrderRepository;

@Component
public class DataInitializer implements ApplicationRunner {

    private final UserAccountRepository userAccountRepository;
    private final SiteRepository siteRepository;
    private final AssetRepository assetRepository;
    private final SensorReadingRepository sensorReadingRepository;
    private final WorkOrderRepository workOrderRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final AlertRepository alertRepository;
    private final PasswordEncoder passwordEncoder;
    private final SiteService siteService;
    private final AssetService assetService;
    private final SensorReadingService sensorReadingService;
    private final WorkOrderService workOrderService;
    private final MaintenanceService maintenanceService;
    private final String dataFile;

    public DataInitializer(
            UserAccountRepository userAccountRepository,
            SiteRepository siteRepository,
            AssetRepository assetRepository,
            SensorReadingRepository sensorReadingRepository,
            WorkOrderRepository workOrderRepository,
            MaintenanceRecordRepository maintenanceRecordRepository,
            AlertRepository alertRepository,
            PasswordEncoder passwordEncoder,
            SiteService siteService,
            AssetService assetService,
            SensorReadingService sensorReadingService,
            WorkOrderService workOrderService,
            MaintenanceService maintenanceService,
            @Value("${energy.demo.data.file}") String dataFile
    ) {
        this.userAccountRepository = userAccountRepository;
        this.siteRepository = siteRepository;
        this.assetRepository = assetRepository;
        this.sensorReadingRepository = sensorReadingRepository;
        this.workOrderRepository = workOrderRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.alertRepository = alertRepository;
        this.passwordEncoder = passwordEncoder;
        this.siteService = siteService;
        this.assetService = assetService;
        this.sensorReadingService = sensorReadingService;
        this.workOrderService = workOrderService;
        this.maintenanceService = maintenanceService;
        this.dataFile = dataFile;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedUsers();
        seedSites();
        seedAssets();
        seedReadings();
        seedWorkflowExamples();
    }

    private void seedUsers() {
        if (userAccountRepository.count() > 0) {
            return;
        }
        userAccountRepository.save(buildUser("admin.ca", "Harper Douglas", "admin123", UserRole.ADMIN, "AB"));
        userAccountRepository.save(buildUser("ops.lead", "Avery Chen", "ops123", UserRole.OPERATIONS_ENGINEER, "AB"));
        userAccountRepository.save(buildUser("morgan.tech", "Morgan Singh", "tech123", UserRole.TECHNICIAN, "AB"));
    }

    private void seedSites() {
        if (siteRepository.count() > 0) {
            return;
        }
        siteService.createOrUpdate(new SiteRequest(
                "SITE-CLY-01",
                "Calgary North Pump Station",
                "Calgary",
                "AB",
                "T2P 1J9",
                "America/Edmonton",
                "ACTIVE",
                "Primary Alberta liquids handling site."
        ), "system");
        siteService.createOrUpdate(new SiteRequest(
                "SITE-CLY-02",
                "Foothills Compression Hub",
                "Cochrane",
                "AB",
                "T4C 1A1",
                "America/Edmonton",
                "ACTIVE",
                "Compression and valve operations for foothills gas movement."
        ), "system");
    }

    private void seedAssets() {
        if (assetRepository.count() > 0) {
            return;
        }
        assetService.createOrUpdate(new ca.yisong.energyops.api.ApiModels.AssetRequest(
                "AST-PMP-101", "Feed Pump A", "Pump", "SITE-CLY-01", "ACTIVE",
                62.0, 15.0, 3.5, 820.0, 980.0, 150.0, 210.0
        ), "system");
        assetService.createOrUpdate(new ca.yisong.energyops.api.ApiModels.AssetRequest(
                "AST-PMP-102", "Feed Pump B", "Pump", "SITE-CLY-01", "ACTIVE",
                63.0, 15.0, 3.5, 820.0, 980.0, 155.0, 210.0
        ), "system");
        assetService.createOrUpdate(new ca.yisong.energyops.api.ApiModels.AssetRequest(
                "AST-CMP-201", "Compressor Train 1", "Compressor", "SITE-CLY-02", "ACTIVE",
                68.0, 12.0, 4.8, 1180.0, 1320.0, 225.0, 340.0
        ), "system");
        assetService.createOrUpdate(new ca.yisong.energyops.api.ApiModels.AssetRequest(
                "AST-VAL-301", "Control Valve 3", "Valve", "SITE-CLY-02", "ACTIVE",
                45.0, 10.0, 2.5, 520.0, 690.0, 20.0, 90.0
        ), "system");
        assetService.createOrUpdate(new ca.yisong.energyops.api.ApiModels.AssetRequest(
                "AST-GEN-401", "Backup Generator 1", "Generator", "SITE-CLY-01", "ACTIVE",
                73.0, 12.0, 3.8, 940.0, 1100.0, 260.0, 150.0
        ), "system");
    }

    private void seedReadings() {
        if (sensorReadingRepository.count() > 0) {
            return;
        }
        sensorReadingService.importCsv(new ClassPathResource(dataFile), "system");
    }

    private void seedWorkflowExamples() {
        if (workOrderRepository.count() > 0 || alertRepository.count() == 0) {
            return;
        }

        Alert vibrationAlert = alertRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(alert -> "High Vibration".equalsIgnoreCase(alert.getAlertType()))
                .findFirst()
                .orElse(null);
        Alert pressureAlert = alertRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(alert -> "Pressure Deviation".equalsIgnoreCase(alert.getAlertType()))
                .findFirst()
                .orElse(null);

        if (vibrationAlert != null) {
            workOrderService.createWorkOrder(new WorkOrderCreateRequest(
                    vibrationAlert.getId(),
                    null,
                    null,
                    "Inspect compressor bearing vibration",
                    "Perform a field vibration survey and confirm alignment tolerance.",
                    "HIGH",
                    "morgan.tech",
                    LocalDate.now().plusDays(1)
            ), "ops.lead");
        }

        if (pressureAlert != null) {
            var completedOrder = workOrderService.createWorkOrder(new WorkOrderCreateRequest(
                    pressureAlert.getId(),
                    null,
                    null,
                    "Validate valve pressure stability",
                    "Check valve position feedback and confirm upstream pressure stability.",
                    "MEDIUM",
                    "morgan.tech",
                    LocalDate.now()
            ), "ops.lead");
            workOrderService.updateWorkOrder(completedOrder.id(), new WorkOrderUpdateRequest(
                    "COMPLETED",
                    "morgan.tech",
                    LocalDate.now(),
                    "Valve recalibrated and stable after retest.",
                    38
            ), "morgan.tech");
            if (maintenanceRecordRepository.count() == 0) {
                maintenanceService.createRecord(new MaintenanceRecordRequest(
                        completedOrder.id(),
                        "Valve position feedback drift",
                        "Recalibrated actuator feedback and completed a post-maintenance pressure check.",
                        38,
                        "Position sensor kit",
                        "Closed with stable readings in Alberta Mountain Time demo window."
                ), "morgan.tech");
            }
        }
    }

    private UserAccount buildUser(String username, String fullName, String rawPassword, UserRole role, String province) {
        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setFullName(fullName);
        account.setPasswordHash(passwordEncoder.encode(rawPassword));
        account.setRole(role);
        account.setHomeProvince(province);
        account.setActive(true);
        return account;
    }
}
