package com.KeyStone.DeliveryService.Repository;

import com.KeyStone.DeliveryService.Entity.Site;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SiteRepository extends JpaRepository<Site, Integer> {

    List<Site> findByCustomerId(Integer customerId);
}