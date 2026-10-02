package com.KeyStone.DeliveryService.Repository;

import com.KeyStone.DeliveryService.Entity.WorkOrder;
import com.KeyStone.DeliveryService.Enum.WorkOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface WorkOrderRepository
        extends JpaRepository<WorkOrder, Integer>, JpaSpecificationExecutor<WorkOrder> {

    Page<WorkOrder> findByCustomerId(Integer customerId, Pageable pageable);

    Page<WorkOrder> findByTechnicianId(Integer technicianId, Pageable pageable);

    long countByStatus(WorkOrderStatus status);

    long countByTechnicianIdAndStatusIn(Integer technicianId, Collection<WorkOrderStatus> statuses);

    /** Active jobs whose SLA falls before the given instant — used by the SLA monitor. */
    List<WorkOrder> findByStatusInAndSlaDueDateBefore(Collection<WorkOrderStatus> statuses, Instant before);
}
