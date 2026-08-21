package com.KeyStone.DeliveryService.Repository;

import com.KeyStone.DeliveryService.Entity.WorkOrder;
import com.KeyStone.DeliveryService.Enum.WorkOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkOrderRepository
        extends JpaRepository<WorkOrder, Integer> {

    Page<WorkOrder> findByCustomerId(
            Integer customerId,
            Pageable pageable
    );

    Page<WorkOrder> findByTechnicianId(
            Integer technicianId,
            Pageable pageable
    );

    // Report summary
    long countByStatus(WorkOrderStatus status);
}