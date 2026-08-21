package com.KeyStone.DeliveryService.Repository;

import com.KeyStone.DeliveryService.Entity.WorkOrderPart;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkOrderPartRepository
        extends JpaRepository<WorkOrderPart, Integer> {
}
