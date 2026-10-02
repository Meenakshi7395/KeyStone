package com.KeyStone.DeliveryService.Service;

import com.KeyStone.DeliveryService.DTO.Site.SiteRequestDTO;
import com.KeyStone.DeliveryService.DTO.Site.SiteResponseDTO;
import com.KeyStone.DeliveryService.Entity.Customer;
import com.KeyStone.DeliveryService.Entity.Site;
import com.KeyStone.DeliveryService.Entity.User;
import com.KeyStone.DeliveryService.Enum.Role;
import com.KeyStone.DeliveryService.Repository.CustomerRepository;
import com.KeyStone.DeliveryService.Repository.SiteRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/** Sites always belong to exactly one customer (F2.2). */
@Service
public class SiteService {

    private final SiteRepository siteRepository;
    private final CustomerRepository customerRepository;

    public SiteService(SiteRepository siteRepository, CustomerRepository customerRepository) {
        this.siteRepository = siteRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public SiteResponseDTO create(User caller, Integer customerId, SiteRequestDTO request) {
        assertCanSeeCustomer(caller, customerId);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found: " + customerId));
        Site site = new Site();
        site.setName(request.name().trim());
        site.setAddress(request.address().trim());
        site.setCustomer(customer);
        return toResponse(siteRepository.save(site));
    }

    @Transactional
    public SiteResponseDTO update(Integer customerId, Integer siteId, SiteRequestDTO request) {
        Site site = getOrThrow(siteId);
        if (!site.getCustomer().getId().equals(customerId)) {
            throw new NoSuchElementException("Site " + siteId + " not found for customer " + customerId);
        }
        site.setName(request.name().trim());
        site.setAddress(request.address().trim());
        return toResponse(site);
    }

    /** Sites of one customer. A customer login may only list its own organisation's sites. */
    @Transactional(readOnly = true)
    public List<SiteResponseDTO> getByCustomerId(User caller, Integer customerId) {
        assertCanSeeCustomer(caller, customerId);
        if (!customerRepository.existsById(customerId)) {
            throw new NoSuchElementException("Customer not found: " + customerId);
        }
        return siteRepository.findByCustomerId(customerId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SiteResponseDTO getById(User caller, Integer siteId) {
        Site site = getOrThrow(siteId);
        assertCanSeeCustomer(caller, site.getCustomer().getId());
        return toResponse(site);
    }

    /** Searchable, paginated list of all sites (staff): GET /api/sites?search=&customerId= */
    @Transactional(readOnly = true)
    public Page<SiteResponseDTO> search(String search, Integer customerId, Pageable pageable) {
        Specification<Site> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (customerId != null) {
                p.add(cb.equal(root.get("customer").get("id"), customerId));
            }
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                p.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("address")), like),
                        cb.like(cb.lower(root.get("customer").get("name")), like)));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        return siteRepository.findAll(spec, pageable).map(this::toResponse);
    }

    private void assertCanSeeCustomer(User caller, Integer customerId) {
        if (caller.getRole() == Role.CUSTOMER
                && (caller.getCustomer() == null || !caller.getCustomer().getId().equals(customerId))) {
            throw new AccessDeniedException("Cannot view sites for another organisation");
        }
        if (caller.getRole() == Role.TECHNICIAN) {
            throw new AccessDeniedException("Technicians see sites through their assigned jobs");
        }
    }

    private Site getOrThrow(Integer id) {
        return siteRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Site not found: " + id));
    }

    private SiteResponseDTO toResponse(Site site) {
        return new SiteResponseDTO(
                site.getId(),
                site.getName(),
                site.getAddress(),
                site.getCreatedAt(),
                site.getCustomer().getId(),
                site.getCustomer().getName());
    }
}
