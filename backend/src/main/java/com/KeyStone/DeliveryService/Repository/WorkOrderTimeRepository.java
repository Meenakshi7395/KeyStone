package com.KeyStone.DeliveryService.Repository;

import com.KeyStone.DeliveryService.Entity.WorkOrderTime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkOrderTimeRepository
        extends JpaRepository<WorkOrderTime, Integer> {

    List<WorkOrderTime> findByWorkOrderIdOrderByCreatedAtDesc(
            Integer workOrderId
    );
}
