package com.KeyStone.DeliveryService.Controller;

import com.KeyStone.DeliveryService.DTO.AuthResponseDTO;
import com.KeyStone.DeliveryService.DTO.LinkCustomerRequestDTO;
import com.KeyStone.DeliveryService.DTO.LoginRequestDTO;
import com.KeyStone.DeliveryService.DTO.RegisterRequestDTO;
import com.KeyStone.DeliveryService.DTO.UserResponseDTO;
import com.KeyStone.DeliveryService.Entity.User;
import com.KeyStone.DeliveryService.Service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO request) {

        return ResponseEntity.ok(
                userService.login(request)
        );
    }

    // Public self-registration. Always creates a CUSTOMER account — staff
    // accounts are created by a manager via POST /api/users instead.
    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(
            @Valid @RequestBody RegisterRequestDTO request) {

        return ResponseEntity.ok(
                userService.registerCustomer(request)
        );
    }

    // One-time self-link from a CUSTOMER login to the organisation
    // (Customer) it represents. See UserService#linkCustomer for why this
    // can only be done once from the client side.
    @PutMapping("/link-customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<UserResponseDTO> linkCustomer(
            Authentication authentication,
            @Valid @RequestBody LinkCustomerRequestDTO request) {

        User caller = (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                userService.linkCustomer(caller.getId(), request.customerId())
        );
    }
}
