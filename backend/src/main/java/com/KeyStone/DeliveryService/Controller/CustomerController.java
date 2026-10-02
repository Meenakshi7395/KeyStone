package com.KeyStone.DeliveryService.Controller;

import com.KeyStone.DeliveryService.DTO.Customer.CustomerRequestDTO;
import com.KeyStone.DeliveryService.DTO.Customer.CustomerResponseDTO;
import com.KeyStone.DeliveryService.Entity.User;
import com.KeyStone.DeliveryService.Service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Customer organisations. Dispatchers and managers manage them; a customer
 * login can read only its own (see CustomerService for the scoping rules).
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
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public ResponseEntity<CustomerResponseDTO> update(@PathVariable Integer id,
                                                      @Valid @RequestBody CustomerRequestDTO request) {
        return ResponseEntity.ok(customerService.update(id, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','CUSTOMER')")
    public ResponseEntity<CustomerResponseDTO> getById(@AuthenticationPrincipal User caller, @PathVariable Integer id) {
        return ResponseEntity.ok(customerService.getById(caller, id));
    }

    /** GET /api/customers?search=harb&page=0&size=10&sort=name,asc */
    @GetMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','CUSTOMER')")
    public ResponseEntity<Page<CustomerResponseDTO>> list(@AuthenticationPrincipal User caller,
                                                          @RequestParam(required = false) String search,
                                                          @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(customerService.list(caller, search, pageable));
    }
}
