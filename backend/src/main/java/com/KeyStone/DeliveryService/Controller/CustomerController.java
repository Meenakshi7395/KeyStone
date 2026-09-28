package com.KeyStone.DeliveryService.Controller;

import com.KeyStone.DeliveryService.DTO.Customer.CustomerRequestDTO;
import com.KeyStone.DeliveryService.DTO.Customer.CustomerResponseDTO;
import com.KeyStone.DeliveryService.Service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Only dispatchers/managers ever reach these — CUSTOMER and
 * TECHNICIAN roles are rejected. Roles are read straight off the
 * JWT (see JWTAuthenticationFilter, which grants "ROLE_<role>"),
 * so this uses hasRole(...) rather than fine-grained authorities.
 */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public ResponseEntity<CustomerResponseDTO> create(@Valid @RequestBody CustomerRequestDTO request) {
        return ResponseEntity.ok(customerService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public ResponseEntity<CustomerResponseDTO> update(
            @PathVariable Integer id,
            @Valid @RequestBody CustomerRequestDTO request) {
        return ResponseEntity.ok(customerService.update(id, request));
    }

    // Also reachable by CUSTOMER: a not-yet-linked customer login needs to
    // search the organisation directory once to find and self-link to its
    // own company (see AuthController#linkCustomer). This is read-only
    // company-directory data (name/contact email), not work-order detail.
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','CUSTOMER')")
    public ResponseEntity<CustomerResponseDTO> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(customerService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','CUSTOMER')")
    public ResponseEntity<Page<CustomerResponseDTO>> list(
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return ResponseEntity.ok(customerService.list(search, pageable));
    }

//    @GetMapping("/test-auth")
//    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
//    public ResponseEntity<?> testAuth(
//            org.springframework.security.core.Authentication authentication) {
//
//        com.KeyStone.DeliveryService.Entity.User user =
//                (com.KeyStone.DeliveryService.Entity.User)
//                        authentication.getPrincipal();
//
//        return ResponseEntity.ok(
//                java.util.Map.of(
//                        "authenticated", authentication.isAuthenticated(),
//                        "email", user.getEmail(),
//                        "role", user.getRole().name(),
//                        "authorities", authentication.getAuthorities()
//                )
//        );
//    }
}
