package com.KeyStone.DeliveryService.Controller;

import com.KeyStone.DeliveryService.DTO.Site.SiteResponseDTO;
import com.KeyStone.DeliveryService.Entity.User;
import com.KeyStone.DeliveryService.Service.SiteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Flat, searchable site list across all customers (F2.3). */
@RestController
@RequestMapping("/api/sites")
public class SiteSearchController {

    private final SiteService siteService;

    public SiteSearchController(SiteService siteService) {
        this.siteService = siteService;
    }

    /** GET /api/sites?search=tower&customerId=1&page=0&size=20&sort=name,asc */
    @GetMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public ResponseEntity<Page<SiteResponseDTO>> search(@RequestParam(required = false) String search,
                                                        @RequestParam(required = false) Integer customerId,
                                                        @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(siteService.search(search, customerId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','CUSTOMER')")
    public ResponseEntity<SiteResponseDTO> getById(@AuthenticationPrincipal User caller, @PathVariable Integer id) {
        return ResponseEntity.ok(siteService.getById(caller, id));
    }
}
