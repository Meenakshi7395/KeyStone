package com.KeyStone.DeliveryService.Service;

import com.KeyStone.DeliveryService.DTO.Part.PartRequestDTO;
import com.KeyStone.DeliveryService.DTO.Part.PartResponseDTO;
import com.KeyStone.DeliveryService.Entity.Part;
import com.KeyStone.DeliveryService.Repository.PartRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PartService {

    private final PartRepository partRepository;

    public PartService(PartRepository partRepository) {
        this.partRepository = partRepository;
    }

    public PartResponseDTO create(PartRequestDTO request) {

        Part part = new Part();

        part.setName(request.name());
        part.setStockQuantity(request.stockQuantity());

        return toResponse(partRepository.save(part));
    }

    public List<PartResponseDTO> getAll() {

        return partRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public PartResponseDTO getById(Integer id) {

        Part part = partRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Part not found: " + id)
                );

        return toResponse(part);
    }

    private PartResponseDTO toResponse(Part part) {

        return new PartResponseDTO(
                part.getId(),
                part.getName(),
                part.getStockQuantity()
        );
    }
}

