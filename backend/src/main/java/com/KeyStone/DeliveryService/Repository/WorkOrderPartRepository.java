package com.KeyStone.DeliveryService.Repository;

import com.KeyStone.DeliveryService.Entity.WorkOrderPart;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkOrderPartRepository
        extends JpaRepository<WorkOrderPart, Integer> {

    List<WorkOrderPart> findByWorkOrderIdOrderByCreatedAtDesc(Integer workOrderId);
}
