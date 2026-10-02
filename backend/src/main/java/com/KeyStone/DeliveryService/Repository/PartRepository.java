package com.KeyStone.DeliveryService.Repository;

import com.KeyStone.DeliveryService.Entity.Part;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PartRepository extends JpaRepository<Part, Integer> {

    /**
     * Row-locks the part (SELECT ... FOR UPDATE) so two technicians logging
     * the same part at once can't both pass the stock check.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Part p where p.id = :id")
    Optional<Part> findByIdForUpdate(@Param("id") Integer id);
}
