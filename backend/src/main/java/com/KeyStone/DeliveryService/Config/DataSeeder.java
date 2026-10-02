package com.KeyStone.DeliveryService.Config;

import com.KeyStone.DeliveryService.Entity.Customer;
import com.KeyStone.DeliveryService.Entity.Part;
import com.KeyStone.DeliveryService.Entity.Site;
import com.KeyStone.DeliveryService.Entity.User;
import com.KeyStone.DeliveryService.Entity.WorkOrder;
import com.KeyStone.DeliveryService.Entity.WorkOrderHistory;
import com.KeyStone.DeliveryService.Entity.WorkOrderPart;
import com.KeyStone.DeliveryService.Entity.WorkOrderTime;
import com.KeyStone.DeliveryService.Enum.Role;
import com.KeyStone.DeliveryService.Enum.WorkOrderPriority;
import com.KeyStone.DeliveryService.Enum.WorkOrderStatus;
import com.KeyStone.DeliveryService.Repository.CustomerRepository;
import com.KeyStone.DeliveryService.Repository.PartRepository;
import com.KeyStone.DeliveryService.Repository.SiteRepository;
import com.KeyStone.DeliveryService.Repository.UserRepository;
import com.KeyStone.DeliveryService.Repository.WorkOrderHistoryRepository;
import com.KeyStone.DeliveryService.Repository.WorkOrderPartRepository;
import com.KeyStone.DeliveryService.Repository.WorkOrderRepository;
import com.KeyStone.DeliveryService.Repository.WorkOrderTimeRepository;
import com.KeyStone.DeliveryService.Service.WorkOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs once at startup (no Flyway for now — schema comes from ddl-auto=update).
 *
 * 1. Small schema fixes ddl-auto can't do on an existing database.
 * 2. Backfills work-order codes (WO-0001) on rows created before codes existed.
 * 3. Seed data (keystone.seed.enabled=true): one login per role plus sample
 *    customers, sites, parts and work orders across the whole lifecycle.
 *    Seed users are created only if their email is missing; sample work
 *    orders only if the work_orders table is empty. Safe to run repeatedly.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    public static final String DEMO_PASSWORD = "Keystone@123";

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final SiteRepository siteRepository;
    private final PartRepository partRepository;
    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderHistoryRepository historyRepository;
    private final WorkOrderPartRepository workOrderPartRepository;
    private final WorkOrderTimeRepository workOrderTimeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;

    @Value("${keystone.seed.enabled:true}")
    private boolean seedEnabled;

    public DataSeeder(UserRepository userRepository,
                      CustomerRepository customerRepository,
                      SiteRepository siteRepository,
                      PartRepository partRepository,
                      WorkOrderRepository workOrderRepository,
                      WorkOrderHistoryRepository historyRepository,
                      WorkOrderPartRepository workOrderPartRepository,
                      WorkOrderTimeRepository workOrderTimeRepository,
                      PasswordEncoder passwordEncoder,
                      JdbcTemplate jdbc) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.siteRepository = siteRepository;
        this.partRepository = partRepository;
        this.workOrderRepository = workOrderRepository;
        this.historyRepository = historyRepository;
        this.workOrderPartRepository = workOrderPartRepository;
        this.workOrderTimeRepository = workOrderTimeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        fixSchema();
        backfillCodes();
        if (seedEnabled) {
            seed();
        }
    }

    // ------------------------------------------------------------------
    // 1. Schema fixes
    // ------------------------------------------------------------------

    private void fixSchema() {
        // Hibernate adds CHECK (status in (...)) when it first creates the table.
        // ddl-auto=update never refreshes it, so an existing database would
        // reject the new CANCELLED status. Drop the stale check; the enum in
        // code (and the service-layer state machine) already guards the value.
        try {
            jdbc.execute("ALTER TABLE work_orders DROP CONSTRAINT IF EXISTS work_orders_status_check");
        } catch (RuntimeException ex) {
            log.warn("Could not drop work_orders_status_check: {}", ex.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // 2. Codes
    // ------------------------------------------------------------------

    private void backfillCodes() {
        int updated = 0;
        for (WorkOrder wo : workOrderRepository.findAll()) {
            if (wo.getCode() == null) {
                wo.setCode(WorkOrderService.formatCode(wo.getId()));
                updated++;
            }
        }
        if (updated > 0) {
            log.info("Backfilled codes on {} existing work orders", updated);
        }
    }

    // ------------------------------------------------------------------
    // 3. Seed data
    // ------------------------------------------------------------------

    private void seed() {
        // --- Customers & sites ---
        Customer harbour = customer("Harbour Point Towers", "ops@harbourpoint.com");
        Customer westgate = customer("Westgate Business Plaza", "fm@westgateplaza.com");
        Customer sunrise = customer("Sunrise Tech Park", "facilities@sunrisetechpark.com");
        Customer lotus = customer("Lotus Medical Centre", "admin@lotusmedical.com");

        // --- Users: one login per role (and a few more for realistic data) ---
        User rajesh = user("Rajesh Malhotra", "rajesh.malhotra@meridian.com", Role.MANAGER, null);
        user("Kavita Iyer", "kavita.iyer@meridian.com", Role.MANAGER, null);
        User priya = user("Priya Sharma", "priya.sharma@meridian.com", Role.DISPATCHER, null);
        user("Neha Kapoor", "neha.kapoor@meridian.com", Role.DISPATCHER, null);
        User arjun = user("Arjun Kumar", "arjun.kumar@meridian.com", Role.TECHNICIAN, null);
        User rohit = user("Rohit Singh", "rohit.singh@meridian.com", Role.TECHNICIAN, null);
        User manoj = user("Manoj Verma", "manoj.verma@meridian.com", Role.TECHNICIAN, null);
        user("Sandeep Yadav", "sandeep.yadav@meridian.com", Role.TECHNICIAN, null);
        user("Ananya Desai", "ananya@harbourpoint.com", Role.CUSTOMER, harbour);
        user("Vikram Mehta", "vikram@westgateplaza.com", Role.CUSTOMER, westgate);
        user("Sneha Reddy", "sneha@sunrisetechpark.com", Role.CUSTOMER, sunrise);
        user("Farhan Qureshi", "farhan@lotusmedical.com", Role.CUSTOMER, lotus);

        // Sites are ensured every start (idempotent), so every demo customer has somewhere to raise requests.
        Site towerA = site(harbour, "Tower A", "12 Harbour Road, Mumbai");
        Site towerB = site(harbour, "Tower B", "14 Harbour Road, Mumbai");
        Site mainBlock = site(westgate, "Main Block", "1 Westgate Avenue, Pune");
        Site plantRoom = site(westgate, "Plant Room", "1 Westgate Avenue (Basement), Pune");
        Site block3 = site(sunrise, "Block 3", "Sunrise Tech Park, Whitefield, Bengaluru");
        Site dataCentre = site(sunrise, "Data Centre", "Sunrise Tech Park, Whitefield, Bengaluru");
        Site opd = site(lotus, "OPD Wing", "Lotus Medical Centre, Sector 18, Noida");

        if (workOrderRepository.count() > 0) {
            log.info("Seed: users/customers ensured; work orders already present, skipping sample jobs");
            return;
        }


        // --- Parts ---
        Part mcb = part("20A MCB breaker", "EL-MCB-20A", "450.00", 40);
        Part contactor = part("Compressor contactor 40A", "HV-CON-40A", "2100.00", 6);
        Part filter = part("AHU filter 24x24", "HV-FLT-2424", "780.00", 3);
        Part valveKit = part("Valve seal kit", "PL-VSK-01", "320.00", 25);
        Part capacitor = part("Run capacitor 45uF", "HV-CAP-45", "650.00", 12);
        Part ledPanel = part("LED panel 2x2 36W", "EL-LED-36", "1250.00", 18);
        part("Pump mechanical seal", "PL-PMS-32", "1850.00", 4);
        part("Refrigerant R410A (kg)", "HV-R410A", "900.00", 0);

        Instant now = Instant.now();

        // --- Work orders across the whole lifecycle ---
        WorkOrder w1 = order("Rooftop HVAC compressor tripping", "Unit 3 trips on high pressure every afternoon.",
                harbour, towerA, WorkOrderPriority.CRITICAL, now.minus(Duration.ofHours(6)), now.minus(Duration.ofHours(2)), rajesh);
        WorkOrder w2 = order("Lobby lighting circuit down", "Half of the lobby lights are out since the weekend.",
                harbour, towerB, WorkOrderPriority.HIGH, now.minus(Duration.ofHours(10)), now.plus(Duration.ofHours(14)), priya);
        WorkOrder w3 = order("Chilled-water riser leak", "Slow leak at the level 4 riser valve.",
                westgate, plantRoom, WorkOrderPriority.HIGH, now.minus(Duration.ofDays(1)), now.plus(Duration.ofHours(1)), priya);
        WorkOrder w4 = order("Breaker fault on DB-3", "DB-3 breaker trips when the pantry is in use.",
                westgate, mainBlock, WorkOrderPriority.MEDIUM, now.minus(Duration.ofDays(2)), now.plus(Duration.ofDays(1)), priya);
        WorkOrder w5 = order("AHU filter replacement", "Quarterly filter change for AHU-2.",
                sunrise, block3, WorkOrderPriority.LOW, now.minus(Duration.ofDays(3)), now.plus(Duration.ofDays(4)), rajesh);
        WorkOrder w6 = order("Server room CRAC alarm", "High temperature alarm on CRAC unit 2.",
                sunrise, dataCentre, WorkOrderPriority.CRITICAL, now.minus(Duration.ofDays(4)), now.minus(Duration.ofDays(4)).plus(Duration.ofHours(4)), priya);
        WorkOrder w7 = order("OPD washroom tap leaking", "Tap in the ground-floor washroom won't shut off fully.",
                lotus, opd, WorkOrderPriority.MEDIUM, now.minus(Duration.ofDays(6)), now.minus(Duration.ofDays(3)), priya);
        WorkOrder w8 = order("Emergency lighting test", "Monthly emergency lighting test for Tower A.",
                harbour, towerA, WorkOrderPriority.LOW, now.minus(Duration.ofDays(8)), now.minus(Duration.ofDays(1)), rajesh);
        WorkOrder w9 = order("Duplicate: lobby lights", "Raised twice by mistake.",
                harbour, towerB, WorkOrderPriority.HIGH, now.minus(Duration.ofHours(9)), now.plus(Duration.ofHours(15)), priya);
        WorkOrder w10 = order("Pump seal failure", "Booster pump 1 seal weeping.",
                westgate, plantRoom, WorkOrderPriority.HIGH, now.minus(Duration.ofDays(1)), now.minus(Duration.ofHours(1)), priya);

        // OPEN: w1 (overdue, unassigned), w2
        // ASSIGNED
        assign(w3, arjun, priya);
        // IN_PROGRESS
        assign(w4, rohit, priya);
        move(w4, WorkOrderStatus.IN_PROGRESS, rohit, null);
        usePart(w4, mcb, 1, rohit);
        logTime(w4, 45, "Isolated DB-3 and traced the fault to the pantry circuit", rohit);
        // ON_HOLD
        assign(w10, manoj, priya);
        move(w10, WorkOrderStatus.IN_PROGRESS, manoj, null);
        move(w10, WorkOrderStatus.ON_HOLD, manoj, "Waiting for a replacement seal");
        logTime(w10, 30, "Diagnosed seal failure", manoj);
        // COMPLETED (on time)
        assign(w5, arjun, rajesh);
        move(w5, WorkOrderStatus.IN_PROGRESS, arjun, null);
        usePart(w5, filter, 2, arjun);
        logTime(w5, 60, "Replaced both filters and reset the pressure switch", arjun);
        move(w5, WorkOrderStatus.COMPLETED, arjun, null);
        // CLOSED (late — breached SLA)
        assign(w6, arjun, priya);
        move(w6, WorkOrderStatus.IN_PROGRESS, arjun, null);
        usePart(w6, contactor, 1, arjun);
        usePart(w6, capacitor, 1, arjun);
        logTime(w6, 150, "Replaced contactor and capacitor, unit stable", arjun);
        move(w6, WorkOrderStatus.COMPLETED, arjun, null);
        move(w6, WorkOrderStatus.CLOSED, rajesh, "Signed off with the customer");
        // CLOSED (on time)
        assign(w7, manoj, priya);
        move(w7, WorkOrderStatus.IN_PROGRESS, manoj, null);
        usePart(w7, valveKit, 1, manoj);
        logTime(w7, 40, "Replaced cartridge and seal", manoj);
        move(w7, WorkOrderStatus.COMPLETED, manoj, null);
        move(w7, WorkOrderStatus.CLOSED, rajesh, null);
        // COMPLETED
        assign(w8, rohit, rajesh);
        move(w8, WorkOrderStatus.IN_PROGRESS, rohit, null);
        usePart(w8, ledPanel, 2, rohit);
        logTime(w8, 90, "Tested all fittings, replaced two failed panels", rohit);
        move(w8, WorkOrderStatus.COMPLETED, rohit, null);
        // CANCELLED
        move(w9, WorkOrderStatus.CANCELLED, priya, "Duplicate of " + w2.getCode());

        // Completion times that make SLA compliance meaningful in the demo.
        stamp(w5, "completed_at", now.minus(Duration.ofDays(1)));
        stamp(w6, "completed_at", now.minus(Duration.ofDays(3)));
        stamp(w6, "closed_at", now.minus(Duration.ofDays(3)).plus(Duration.ofHours(2)));
        stamp(w7, "completed_at", now.minus(Duration.ofDays(4)));
        stamp(w7, "closed_at", now.minus(Duration.ofDays(4)).plus(Duration.ofHours(3)));
        stamp(w8, "completed_at", now.minus(Duration.ofDays(2)));

        log.info("Seed: created sample customers, sites, parts and {} work orders. Demo password: {}",
                workOrderRepository.count(), DEMO_PASSWORD);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private final Map<String, Customer> customerCache = new HashMap<>();

    private Customer customer(String name, String email) {
        return customerCache.computeIfAbsent(name, n -> {
            List<Customer> existing = customerRepository.findByNameContainingIgnoreCase(n,
                    org.springframework.data.domain.PageRequest.of(0, 5)).getContent();
            for (Customer c : existing) {
                if (c.getName().equalsIgnoreCase(n)) {
                    return c;
                }
            }
            Customer c = new Customer();
            c.setName(n);
            c.setContactEmail(email);
            return customerRepository.save(c);
        });
    }

    private User user(String name, String email, Role role, Customer customer) {
        return userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            User u = new User();
            u.setName(name);
            u.setEmail(email);
            u.setRole(role);
            u.setCustomer(customer);
            u.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
            log.info("Seed user created: {} ({})", email, role);
            return userRepository.save(u);
        });
    }

    private Site site(Customer customer, String name, String address) {
        for (Site existing : siteRepository.findByCustomerId(customer.getId())) {
            if (existing.getName().equalsIgnoreCase(name)) {
                return existing;
            }
        }
        Site s = new Site();
        s.setCustomer(customer);
        s.setName(name);
        s.setAddress(address);
        return siteRepository.save(s);
    }

    private Part part(String name, String sku, String unitCost, int stock) {
        Part p = new Part(name, stock);
        p.setSku(sku);
        p.setUnitCost(new BigDecimal(unitCost));
        return partRepository.save(p);
    }

    private WorkOrder order(String title, String description, Customer customer, Site site,
                            WorkOrderPriority priority, Instant createdAt, Instant due, User raisedBy) {
        WorkOrder wo = new WorkOrder();
        wo.setTitle(title);
        wo.setDescription(description);
        wo.setCustomer(customer);
        wo.setSite(site);
        wo.setPriority(priority);
        wo.setStatus(WorkOrderStatus.OPEN);
        wo.setSlaDueDate(due);
        WorkOrder saved = workOrderRepository.saveAndFlush(wo);
        saved.setCode(WorkOrderService.formatCode(saved.getId()));
        history(saved, raisedBy, "WORK_ORDER_CREATED", null, title, null);
        stamp(saved, "created_at", createdAt); // created_at is not updatable through JPA
        saved.setCreatedAt(createdAt);          // keep the managed copy in step
        return saved;
    }

    private void assign(WorkOrder wo, User tech, User by) {
        wo.setTechnician(tech);
        if (wo.getStatus() == WorkOrderStatus.OPEN) {
            wo.setStatus(WorkOrderStatus.ASSIGNED);
            history(wo, by, "STATUS_CHANGED", "OPEN", "ASSIGNED", null);
        }
        history(wo, by, "TECHNICIAN_ASSIGNED", null, tech.getName(), null);
    }

    private void move(WorkOrder wo, WorkOrderStatus to, User by, String note) {
        WorkOrderStatus from = wo.getStatus();
        wo.setStatus(to);
        if (to == WorkOrderStatus.COMPLETED) {
            wo.setCompletedAt(Instant.now());
        } else if (to == WorkOrderStatus.CLOSED) {
            wo.setClosedAt(Instant.now());
        }
        history(wo, by, "STATUS_CHANGED", from.name(), to.name(), note);
    }

    private void usePart(WorkOrder wo, Part part, int qty, User by) {
        part.setStockQuantity(Math.max(0, part.getStockQuantity() - qty));
        WorkOrderPart line = new WorkOrderPart();
        line.setWorkOrder(wo);
        line.setPart(part);
        line.setQuantity(qty);
        line.setUnitCost(part.getUnitCost());
        line.setLoggedBy(by);
        workOrderPartRepository.save(line);
        history(wo, by, "PART_USED", part.getName(), "Quantity: " + qty, null);
    }

    private void logTime(WorkOrder wo, int minutes, String note, User by) {
        WorkOrderTime t = new WorkOrderTime();
        t.setWorkOrder(wo);
        t.setMinutes(minutes);
        t.setHours(Math.round(minutes / 60f));
        t.setDescription(note);
        t.setLoggedBy(by);
        workOrderTimeRepository.save(t);
        history(wo, by, "TIME_LOGGED", null, minutes + " min", note);
    }

    private void history(WorkOrder wo, User by, String action, String oldValue, String newValue, String note) {
        WorkOrderHistory h = new WorkOrderHistory();
        h.setWorkOrder(wo);
        h.setChangedBy(by);
        h.setAction(action);
        h.setOldValue(oldValue);
        h.setNewValue(newValue);
        h.setNote(note);
        historyRepository.save(h);
    }

    /** Direct column update for seeded timestamps (flushes pending JPA changes first). */
    private void stamp(WorkOrder wo, String column, Instant value) {
        workOrderRepository.flush();
        jdbc.update("UPDATE work_orders SET " + column + " = ? WHERE id = ?", Timestamp.from(value), wo.getId());
    }
}
