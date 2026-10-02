package com.KeyStone.DeliveryService.Controller;

import com.KeyStone.DeliveryService.DTO.Site.SiteRequestDTO;
import com.KeyStone.DeliveryService.DTO.Site.SiteResponseDTO;
import com.KeyStone.DeliveryService.Entity.User;
import com.KeyStone.DeliveryService.Service.SiteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Sites nested under their customer: /api/customers/{customerId}/sites */
@RestController
@RequestMapping("/api/customers/{customerId}/sites")
public class SiteController {

    private final SiteService siteService;

    public SiteController(SiteService siteService) {
        this.siteService = siteService;
    }

    /** F2: dispatchers and managers create sites for a customer. */
    @PostMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public ResponseEntity<SiteResponseDTO> createSite(@AuthenticationPrincipal User caller,
                                                      @PathVariable Integer customerId,
                                                      @Valid @RequestBody SiteRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(siteService.create(caller, customerId, request));
    }

    @PutMapping("/{siteId}")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public ResponseEntity<SiteResponseDTO> updateSite(@PathVariable Integer customerId,
                                                      @PathVariable Integer siteId,
                                                      @Valid @RequestBody SiteRequestDTO request) {
        return ResponseEntity.ok(siteService.update(customerId, siteId, request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','CUSTOMER')")
    public ResponseEntity<List<SiteResponseDTO>> getSitesByCustomer(@AuthenticationPrincipal User caller,
                                                                    @PathVariable Integer customerId) {
        return ResponseEntity.ok(siteService.getByCustomerId(caller, customerId));
    }
}
