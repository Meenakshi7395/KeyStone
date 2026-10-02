package com.KeyStone.DeliveryService.Repository;

import com.KeyStone.DeliveryService.Entity.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface SiteRepository extends JpaRepository<Site, Integer>, JpaSpecificationExecutor<Site> {

    List<Site> findByCustomerId(Integer customerId);
}
