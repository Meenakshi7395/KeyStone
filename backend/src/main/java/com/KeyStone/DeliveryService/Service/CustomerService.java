package com.KeyStone.DeliveryService.Service;

import com.KeyStone.DeliveryService.DTO.Customer.CustomerRequestDTO;
import com.KeyStone.DeliveryService.DTO.Customer.CustomerResponseDTO;
import com.KeyStone.DeliveryService.Entity.Customer;
import com.KeyStone.DeliveryService.Repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        customer.setName(request.name());
        customer.setContactEmail(request.contactEmail());
        return toResponse(customerRepository.save(customer));
    }

    @Transactional
    public CustomerResponseDTO update(Integer id, CustomerRequestDTO request) {
        Customer customer = getOrThrow(id);
        customer.setName(request.name());
        customer.setContactEmail(request.contactEmail());
        return toResponse(customer);
    }

    public CustomerResponseDTO getById(Integer id) {
        return toResponse(getOrThrow(id));
    }

    public Page<CustomerResponseDTO> list(String search, Pageable pageable) {
        Page<Customer> page = (search == null || search.isBlank())
                ? customerRepository.findAll(pageable)
                : customerRepository.findByNameContainingIgnoreCase(search, pageable);
        return page.map(this::toResponse);
    }

    private Customer getOrThrow(Integer id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Customer not found: " + id));
    }

    private CustomerResponseDTO toResponse(Customer customer) {
        return new CustomerResponseDTO(
                customer.getId(),
                customer.getName(),
                customer.getContactEmail(),
                customer.getCreatedAt());
    }
}
