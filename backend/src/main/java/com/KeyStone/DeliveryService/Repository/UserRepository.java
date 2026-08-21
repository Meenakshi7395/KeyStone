package com.KeyStone.DeliveryService.Repository;

import com.KeyStone.DeliveryService.Entity.User;
import com.KeyStone.DeliveryService.Enum.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository
        extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // Report summary
    long countByRole(Role role);
}
