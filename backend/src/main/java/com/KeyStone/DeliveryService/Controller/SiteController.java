package com.KeyStone.DeliveryService.Controller;

import com.KeyStone.DeliveryService.DTO.Site.SiteRequestDTO;
import com.KeyStone.DeliveryService.DTO.Site.SiteResponseDTO;
import com.KeyStone.DeliveryService.Entity.User;
import com.KeyStone.DeliveryService.Service.SiteService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers/{customerId}/sites")
public class SiteController {

    private final SiteService siteService;

    public SiteController(SiteService siteService) {
        this.siteService = siteService;
    }

    // CREATE SITE
    @PostMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public ResponseEntity<SiteResponseDTO> createSite(
            @PathVariable Integer customerId,
            @Valid @RequestBody SiteRequestDTO request) {

        return ResponseEntity.ok(
                siteService.create(customerId, request)
        );
    }

    // GET ALL SITES FOR CUSTOMER
    // CUSTOMER may only ever list sites for its own linked organisation —
    // needed so a customer-portal user can pick a site when raising a
    // request. Everyone else (dispatcher/manager) can list any customer's.
    @GetMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','CUSTOMER')")
    public ResponseEntity<List<SiteResponseDTO>> getSitesByCustomer(
            Authentication authentication,
            @PathVariable Integer customerId) {

        User caller = (User) authentication.getPrincipal();

        if (caller.getRole().name().equals("CUSTOMER")
                && (caller.getCustomer() == null || !caller.getCustomer().getId().equals(customerId))) {
            throw new AccessDeniedException("Cannot view sites for another organisation");
        }

        return ResponseEntity.ok(
                siteService.getByCustomerId(customerId)
        );
    }
}