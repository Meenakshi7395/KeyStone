package com.KeyStone.DeliveryService.Controller;

import com.KeyStone.DeliveryService.DTO.AuthResponseDTO;
import com.KeyStone.DeliveryService.DTO.LoginRequestDTO;
import com.KeyStone.DeliveryService.Service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
}