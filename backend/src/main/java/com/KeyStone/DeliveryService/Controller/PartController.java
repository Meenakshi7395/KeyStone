package com.KeyStone.DeliveryService.Controller;

import com.KeyStone.DeliveryService.DTO.Part.PartRequestDTO;
import com.KeyStone.DeliveryService.DTO.Part.PartResponseDTO;
import com.KeyStone.DeliveryService.Service.PartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parts")
public class PartController {

    private final PartService partService;

    public PartController(PartService partService) {
        this.partService = partService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PartResponseDTO create(
            @Valid @RequestBody PartRequestDTO request) {

        return partService.create(request);
    }

    @GetMapping
    public List<PartResponseDTO> getAll() {

        return partService.getAll();
    }

    @GetMapping("/{id}")
    public PartResponseDTO getById(
            @PathVariable Integer id) {

        return partService.getById(id);
    }
}

