package com.KeyStone.DeliveryService.Controller;

import com.KeyStone.DeliveryService.DTO.UserRequestDTO;
import com.KeyStone.DeliveryService.DTO.UserResponseDTO;
import com.KeyStone.DeliveryService.Entity.User;
import com.KeyStone.DeliveryService.Service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Staff/account administration — creating dispatchers, technicians,
 * managers, and (rarely) customer accounts on someone's behalf. This is
 * an admin surface, so every endpoint here is MANAGER-only. Public
 * self-registration lives separately at POST /api/auth/register and is
 * hard-limited to the CUSTOMER role there.
 */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('MANAGER')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // CREATE
    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(
            @RequestBody UserRequestDTO request) {

        User user = new User();

        user.setName(request.name());
        user.setEmail(request.email());
        user.setPasswordHash(request.password());
        user.setRole(request.role());

        return ResponseEntity.ok(
                userService.createUser(user)
        );
    }

    // GET ALL
    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {

        return ResponseEntity.ok(
                userService.getAllUsers()
        );
    }

    // GET BY ID
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(
            @PathVariable Integer id) {

        return ResponseEntity.ok(
                userService.getUserById(id)
        );
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(
            @PathVariable Integer id,
            @RequestBody User user) {

        return ResponseEntity.ok(
                userService.updateUser(id, user)
        );
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Integer id) {

        userService.deleteUser(id);

        return ResponseEntity.noContent().build();
    }
}