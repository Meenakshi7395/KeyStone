package com.KeyStone.DeliveryService.Controller;

import com.KeyStone.DeliveryService.DTO.Site.SiteRequestDTO;
import com.KeyStone.DeliveryService.DTO.Site.SiteResponseDTO;
import com.KeyStone.DeliveryService.Service.SiteService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @GetMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public ResponseEntity<List<SiteResponseDTO>> getSitesByCustomer(
            @PathVariable Integer customerId) {

        return ResponseEntity.ok(
                siteService.getByCustomerId(customerId)
        );
    }
}