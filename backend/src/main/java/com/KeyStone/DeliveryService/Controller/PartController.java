package com.KeyStone.DeliveryService.Controller;

import com.KeyStone.DeliveryService.DTO.Part.PartRequestDTO;
import com.KeyStone.DeliveryService.DTO.Part.PartResponseDTO;
import com.KeyStone.DeliveryService.DTO.Part.RestockRequestDTO;
import com.KeyStone.DeliveryService.Service.PartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Parts catalogue: staff and technicians can read it; only managers change it (brief 3.1). */
@RestController
@RequestMapping("/api/parts")
public class PartController {

    private final PartService partService;

    public PartController(PartService partService) {
        this.partService = partService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','TECHNICIAN')")
    public ResponseEntity<List<PartResponseDTO>> getAll() {
        return ResponseEntity.ok(partService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','TECHNICIAN')")
    public ResponseEntity<PartResponseDTO> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(partService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<PartResponseDTO> create(@Valid @RequestBody PartRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(partService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<PartResponseDTO> update(@PathVariable Integer id, @Valid @RequestBody PartRequestDTO request) {
        return ResponseEntity.ok(partService.update(id, request));
    }

    @PostMapping("/{id}/restock")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<PartResponseDTO> restock(@PathVariable Integer id, @Valid @RequestBody RestockRequestDTO request) {
        return ResponseEntity.ok(partService.restock(id, request));
    }
}
