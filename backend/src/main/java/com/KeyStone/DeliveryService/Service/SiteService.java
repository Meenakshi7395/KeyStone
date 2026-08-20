package com.KeyStone.DeliveryService.Service;

import com.KeyStone.DeliveryService.DTO.Site.SiteRequestDTO;
import com.KeyStone.DeliveryService.DTO.Site.SiteResponseDTO;
import com.KeyStone.DeliveryService.Entity.Customer;
import com.KeyStone.DeliveryService.Entity.Site;
import com.KeyStone.DeliveryService.Repository.CustomerRepository;
import com.KeyStone.DeliveryService.Repository.SiteRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class SiteService {

    private final SiteRepository siteRepository;
    private final CustomerRepository customerRepository;

    public SiteService(
            SiteRepository siteRepository,
            CustomerRepository customerRepository) {

        this.siteRepository = siteRepository;
        this.customerRepository = customerRepository;
    }

    // CREATE SITE FOR A CUSTOMER
    @Transactional
    public SiteResponseDTO create(
            Integer customerId,
            SiteRequestDTO request) {

        Customer customer = customerRepository
                .findById(customerId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Customer not found: " + customerId
                        )
                );

        Site site = new Site();

        site.setName(request.name());
        site.setAddress(request.address());
        site.setCustomer(customer);

        return toResponse(
                siteRepository.save(site)
        );
    }

    // GET ALL SITES FOR A CUSTOMER
    public List<SiteResponseDTO> getByCustomerId(
            Integer customerId) {

        // First verify customer exists
        if (!customerRepository.existsById(customerId)) {
            throw new NoSuchElementException(
                    "Customer not found: " + customerId
            );
        }

        return siteRepository
                .findByCustomerId(customerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // CONVERT ENTITY TO RESPONSE DTO
    private SiteResponseDTO toResponse(Site site) {

        return new SiteResponseDTO(
                site.getId(),
                site.getName(),
                site.getAddress(),
                site.getCreatedAt(),
                site.getCustomer().getId()
        );
    }
}