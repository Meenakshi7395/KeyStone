package com.KeyStone.DeliveryService.Service;

import com.KeyStone.DeliveryService.DTO.Customer.CustomerRequestDTO;
import com.KeyStone.DeliveryService.DTO.Customer.CustomerResponseDTO;
import com.KeyStone.DeliveryService.Entity.Customer;
import com.KeyStone.DeliveryService.Entity.User;
import com.KeyStone.DeliveryService.Enum.Role;
import com.KeyStone.DeliveryService.Repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public CustomerResponseDTO create(CustomerRequestDTO request) {
        Customer customer = new Customer();
        customer.setName(request.name().trim());
        customer.setContactEmail(request.contactEmail().trim());
        return toResponse(customerRepository.save(customer), true);
    }

    @Transactional
    public CustomerResponseDTO update(Integer id, CustomerRequestDTO request) {
        Customer customer = getOrThrow(id);
        customer.setName(request.name().trim());
        customer.setContactEmail(request.contactEmail().trim());
        return toResponse(customer, true);
    }

    /** A customer login may only read its own organisation (F2.4). */
    @Transactional(readOnly = true)
    public CustomerResponseDTO getById(User caller, Integer id) {
        if (caller.getRole() == Role.CUSTOMER
                && (caller.getCustomer() == null || !caller.getCustomer().getId().equals(id))) {
            throw new AccessDeniedException("Cannot view another organisation");
        }
        return toResponse(getOrThrow(id), true);
    }

    /**
     * Staff see the full, searchable, paginated directory.
     * A customer login sees only its own organisation (nothing until a manager links it).
     */
    @Transactional(readOnly = true)
    public Page<CustomerResponseDTO> list(User caller, String search, Pageable pageable) {
        if (caller.getRole() == Role.CUSTOMER) {
            if (caller.getCustomer() != null) {
                Customer own = getOrThrow(caller.getCustomer().getId());
                return new PageImpl<>(List.of(toResponse(own, true)), pageable, 1);
            }
            // Not linked to an organisation yet: nothing to show until a manager links the account.
            return Page.empty(pageable);
        }

        Page<Customer> page = (search == null || search.isBlank())
                ? customerRepository.findAll(pageable)
                : customerRepository.findByNameContainingIgnoreCase(search.trim(), pageable);
        return page.map(c -> toResponse(c, true));
    }

    private Customer getOrThrow(Integer id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Customer not found: " + id));
    }

    private CustomerResponseDTO toResponse(Customer customer, boolean includeContact) {
        return new CustomerResponseDTO(
                customer.getId(),
                customer.getName(),
                includeContact ? customer.getContactEmail() : null,
                customer.getCreatedAt());
    }
}
