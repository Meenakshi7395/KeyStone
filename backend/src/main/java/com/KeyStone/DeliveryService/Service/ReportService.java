package com.KeyStone.DeliveryService.Service;

import com.KeyStone.DeliveryService.DTO.Report.ReportSummaryResponseDTO;
import com.KeyStone.DeliveryService.DTO.Report.ReportSummaryResponseDTO.BreakdownRow;
import com.KeyStone.DeliveryService.DTO.WorkOrder.WorkOrderFilter;
import com.KeyStone.DeliveryService.Entity.WorkOrder;
import com.KeyStone.DeliveryService.Enum.Role;
import com.KeyStone.DeliveryService.Enum.SlaState;
import com.KeyStone.DeliveryService.Enum.WorkOrderStatus;
import com.KeyStone.DeliveryService.Repository.CustomerRepository;
import com.KeyStone.DeliveryService.Repository.UserRepository;
import com.KeyStone.DeliveryService.Repository.WorkOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Manager dashboard metrics (F8). Every number is computed from the same
 * filtered set of work orders, so the figures always agree with the filters.
 */
@Service
public class ReportService {

    private final WorkOrderRepository workOrderRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    public ReportService(WorkOrderRepository workOrderRepository,
                         CustomerRepository customerRepository,
                         UserRepository userRepository) {
        this.workOrderRepository = workOrderRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public ReportSummaryResponseDTO getSummary(LocalDate from, LocalDate to,
                                               Integer siteId, Integer technicianId, Integer customerId) {
        WorkOrderFilter filter = new WorkOrderFilter(null, null, technicianId, null, siteId, customerId,
                null, null, from, to);
        List<WorkOrder> orders = workOrderRepository.findAll(WorkOrderSpecifications.build(null, filter));
        Instant now = Instant.now();

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (WorkOrderStatus s : WorkOrderStatus.values()) {
            byStatus.put(s.name(), 0L);
        }

        long active = 0, overdue = 0, atRisk = 0, met = 0, breached = 0;
        long resolvedCount = 0;
        double resolvedHours = 0;

        for (WorkOrder wo : orders) {
            byStatus.merge(wo.getStatus().name(), 1L, Long::sum);
            if (!wo.getStatus().isFinished()) {
                active++;
            }
            if (SlaCalculator.isOverdue(wo, now)) {
                overdue++;
            }
            if (SlaCalculator.isAtRisk(wo, now)) {
                atRisk++;
            }
            SlaOutcome outcome = outcome(wo, now);
            if (outcome == SlaOutcome.MET) {
                met++;
            } else if (outcome == SlaOutcome.BREACHED) {
                breached++;
            }
            if (wo.getCompletedAt() != null && wo.getCreatedAt() != null) {
                resolvedCount++;
                resolvedHours += Duration.between(wo.getCreatedAt(), wo.getCompletedAt()).toMinutes() / 60.0;
            }
        }

        Double compliance = (met + breached) == 0 ? null : round1(met * 100.0 / (met + breached));
        Double avgResolution = resolvedCount == 0 ? null : round1(resolvedHours / resolvedCount);

        return new ReportSummaryResponseDTO(
                orders.size(),
                byStatus.get("OPEN"),
                byStatus.get("ASSIGNED"),
                byStatus.get("IN_PROGRESS"),
                byStatus.get("COMPLETED"),
                customerRepository.count(),
                userRepository.countByRole(Role.TECHNICIAN),
                byStatus.get("ON_HOLD"),
                byStatus.get("CLOSED"),
                byStatus.get("CANCELLED"),
                active,
                overdue,
                atRisk,
                met,
                breached,
                compliance,
                avgResolution,
                byStatus,
                breakdown(orders, now,
                        wo -> wo.getTechnician() != null ? wo.getTechnician().getId() : null,
                        wo -> wo.getTechnician() != null ? wo.getTechnician().getName() : "Unassigned"),
                breakdown(orders, now,
                        wo -> wo.getSite().getId(),
                        wo -> wo.getSite().getName() + " (" + wo.getCustomer().getName() + ")"),
                now
        );
    }

    private enum SlaOutcome { MET, BREACHED, PENDING }

    /** Finished jobs are judged on completion time; open jobs count only once they're already late. */
    private static SlaOutcome outcome(WorkOrder wo, Instant now) {
        SlaState state = SlaCalculator.stateOf(wo, now);
        return switch (state) {
            case MET -> SlaOutcome.MET;
            case BREACHED -> SlaOutcome.BREACHED;
            default -> SlaOutcome.PENDING;
        };
    }

    private static List<BreakdownRow> breakdown(List<WorkOrder> orders, Instant now,
                                                Function<WorkOrder, Integer> idOf,
                                                Function<WorkOrder, String> nameOf) {
        Map<String, long[]> counts = new LinkedHashMap<>(); // total, active, completed, overdue, met, breached
        Map<String, Integer> ids = new LinkedHashMap<>();
        for (WorkOrder wo : orders) {
            String name = nameOf.apply(wo);
            ids.putIfAbsent(name, idOf.apply(wo));
            long[] c = counts.computeIfAbsent(name, k -> new long[6]);
            c[0]++;
            if (!wo.getStatus().isFinished()) {
                c[1]++;
            }
            if (wo.getStatus() == WorkOrderStatus.COMPLETED || wo.getStatus() == WorkOrderStatus.CLOSED) {
                c[2]++;
            }
            if (SlaCalculator.isOverdue(wo, now)) {
                c[3]++;
            }
            SlaOutcome o = outcome(wo, now);
            if (o == SlaOutcome.MET) {
                c[4]++;
            } else if (o == SlaOutcome.BREACHED) {
                c[5]++;
            }
        }
        List<BreakdownRow> rows = new ArrayList<>();
        counts.forEach((name, c) -> rows.add(new BreakdownRow(ids.get(name), name, c[0], c[1], c[2], c[3], c[4], c[5])));
        rows.sort(Comparator.comparingLong(BreakdownRow::overdue).reversed()
                .thenComparing(Comparator.comparingLong(BreakdownRow::active).reversed())
                .thenComparing(BreakdownRow::name));
        return rows;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
