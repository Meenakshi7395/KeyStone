package com.KeyStone.DeliveryService.Service;

import com.KeyStone.DeliveryService.Entity.User;
import com.KeyStone.DeliveryService.Entity.WorkOrder;
import com.KeyStone.DeliveryService.Entity.WorkOrderHistory;
import com.KeyStone.DeliveryService.Enum.NotificationType;
import com.KeyStone.DeliveryService.Enum.Role;
import com.KeyStone.DeliveryService.Enum.WorkOrderStatus;
import com.KeyStone.DeliveryService.Repository.WorkOrderHistoryRepository;
import com.KeyStone.DeliveryService.Repository.WorkOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Scheduled SLA check (brief F7.2 / F7.3).
 *
 * Every few minutes it scans active work orders and:
 *  - flags jobs that are AT RISK (close to their deadline) — dispatchers and
 *    the assigned technician are notified once;
 *  - flags jobs that have BREACHED their deadline — managers, dispatchers and
 *    the technician are notified once, and an audit row is written.
 *
 * The "notified at" timestamps on the work order make each alert fire only
 * once per deadline (editing the deadline resets them).
 */
@Service
public class SlaMonitor {

    private static final Logger log = LoggerFactory.getLogger(SlaMonitor.class);

    private static final List<WorkOrderStatus> ACTIVE = List.of(
            WorkOrderStatus.OPEN, WorkOrderStatus.ASSIGNED, WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.ON_HOLD);

    /** Only jobs due within this horizon can be at risk; keeps the scan small. */
    private static final Duration HORIZON = Duration.ofDays(30);

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderHistoryRepository historyRepository;
    private final NotificationService notificationService;

    public SlaMonitor(WorkOrderRepository workOrderRepository,
                      WorkOrderHistoryRepository historyRepository,
                      NotificationService notificationService) {
        this.workOrderRepository = workOrderRepository;
        this.historyRepository = historyRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(
            initialDelayString = "${keystone.sla.initial-delay-ms:30000}",
            fixedDelayString = "${keystone.sla.check-interval-ms:300000}")
    @Transactional // invoked through the Spring proxy, so the whole pass is one transaction
    public void scheduledCheck() {
        runCheck();
    }

    /** Runs one pass. Public so a manager can trigger it on demand (POST /api/reports/sla-check). */
    @Transactional
    public Result runCheck() {
        Instant now = Instant.now();
        int atRisk = 0;
        int breached = 0;

        for (WorkOrder wo : workOrderRepository.findByStatusInAndSlaDueDateBefore(ACTIVE, now.plus(HORIZON))) {
            if (SlaCalculator.isOverdue(wo, now)) {
                if (wo.getSlaBreachNotifiedAt() == null) {
                    wo.setSlaBreachNotifiedAt(now);
                    audit(wo, "SLA_BREACHED", "Due " + wo.getSlaDueDate());
                    notificationService.notifyRoles(
                            List.of(Role.MANAGER, Role.DISPATCHER),
                            technicianOf(wo),
                            NotificationType.SLA_BREACHED,
                            "SLA breached: " + wo.getCode(),
                            wo.getTitle() + " at " + wo.getSite().getName() + " is past its deadline",
                            wo);
                    log.warn("SLA BREACH on work order {} (due {}, status {})", wo.getCode(), wo.getSlaDueDate(), wo.getStatus());
                    breached++;
                }
            } else if (SlaCalculator.isAtRisk(wo, now) && wo.getSlaRiskNotifiedAt() == null) {
                wo.setSlaRiskNotifiedAt(now);
                audit(wo, "SLA_AT_RISK", "Due " + wo.getSlaDueDate());
                notificationService.notifyRoles(
                        List.of(Role.DISPATCHER),
                        technicianOf(wo),
                        NotificationType.SLA_AT_RISK,
                        "SLA at risk: " + wo.getCode(),
                        wo.getTitle() + " is close to its deadline",
                        wo);
                log.info("SLA at risk on work order {} (due {})", wo.getCode(), wo.getSlaDueDate());
                atRisk++;
            }
        }

        if (atRisk > 0 || breached > 0) {
            log.info("SLA check: {} newly at risk, {} newly breached", atRisk, breached);
        }
        return new Result(atRisk, breached, now);
    }

    private static List<User> technicianOf(WorkOrder wo) {
        List<User> extra = new ArrayList<>();
        if (wo.getTechnician() != null) {
            extra.add(wo.getTechnician());
        }
        return extra;
    }

    private void audit(WorkOrder wo, String action, String value) {
        WorkOrderHistory h = new WorkOrderHistory();
        h.setWorkOrder(wo);
        h.setAction(action);
        h.setNewValue(value);
        h.setNote("Raised by the SLA monitor");
        historyRepository.save(h); // changedBy = null → shown as "System"
    }

    public record Result(int newlyAtRisk, int newlyBreached, Instant checkedAt) {
    }
}
