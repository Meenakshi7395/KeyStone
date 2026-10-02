package com.KeyStone.DeliveryService.Repository;

import com.KeyStone.DeliveryService.Entity.WorkOrderHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkOrderHistoryRepository extends JpaRepository<WorkOrderHistory, Integer> {

    List<WorkOrderHistory> findByWorkOrderIdOrderByCreatedAtDesc(Integer workOrderId);
}
