package com.KeyStone.DeliveryService.Service;

import com.KeyStone.DeliveryService.DTO.Part.PartRequestDTO;
import com.KeyStone.DeliveryService.DTO.Part.PartResponseDTO;
import com.KeyStone.DeliveryService.DTO.Part.RestockRequestDTO;
import com.KeyStone.DeliveryService.Entity.Part;
import com.KeyStone.DeliveryService.Repository.PartRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/** Parts inventory. Stock is only decremented by WorkOrderService#addPart. */
@Service
public class PartService {

    private static final Logger log = LoggerFactory.getLogger(PartService.class);

    private final PartRepository partRepository;

    public PartService(PartRepository partRepository) {
        this.partRepository = partRepository;
    }

    @Transactional
    public PartResponseDTO create(PartRequestDTO request) {
        Part part = new Part();
        apply(part, request);
        part.setStockQuantity(request.stockQuantity());
        Part saved = partRepository.save(part);
        log.info("Part {} '{}' created with stock {}", saved.getId(), saved.getName(), saved.getStockQuantity());
        return toResponse(saved);
    }

    /** Edit name / SKU / unit cost and set the stock level (stock count correction). */
    @Transactional
    public PartResponseDTO update(Integer id, PartRequestDTO request) {
        Part part = getOrThrow(id);
        apply(part, request);
        if (!request.stockQuantity().equals(part.getStockQuantity())) {
            log.info("Part {} stock corrected {} -> {}", id, part.getStockQuantity(), request.stockQuantity());
            part.setStockQuantity(request.stockQuantity());
        }
        return toResponse(part);
    }

    /** Goods received: add to stock under a row lock. */
    @Transactional
    public PartResponseDTO restock(Integer id, RestockRequestDTO request) {
        Part part = partRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NoSuchElementException("Part not found: " + id));
        part.setStockQuantity(part.getStockQuantity() + request.quantity());
        log.info("Part {} restocked +{} (now {})", id, request.quantity(), part.getStockQuantity());
        return toResponse(part);
    }

    @Transactional(readOnly = true)
    public List<PartResponseDTO> getAll() {
        return partRepository.findAll(Sort.by("name")).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PartResponseDTO getById(Integer id) {
        return toResponse(getOrThrow(id));
    }

    private void apply(Part part, PartRequestDTO request) {
        part.setName(request.name().trim());
        part.setSku(request.sku() == null || request.sku().isBlank() ? null : request.sku().trim());
        part.setUnitCost(request.unitCost());
    }

    private Part getOrThrow(Integer id) {
        return partRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Part not found: " + id));
    }

    private PartResponseDTO toResponse(Part part) {
        return new PartResponseDTO(part.getId(), part.getName(), part.getStockQuantity(), part.getSku(), part.getUnitCost());
    }
}
