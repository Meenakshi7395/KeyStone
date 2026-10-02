package com.KeyStone.DeliveryService.Service;

import com.KeyStone.DeliveryService.DTO.AuthResponseDTO;
import com.KeyStone.DeliveryService.DTO.LoginRequestDTO;
import com.KeyStone.DeliveryService.DTO.UserRequestDTO;
import com.KeyStone.DeliveryService.DTO.UserResponseDTO;
import com.KeyStone.DeliveryService.Entity.Customer;
import com.KeyStone.DeliveryService.Entity.User;
import com.KeyStone.DeliveryService.Enum.Role;
import com.KeyStone.DeliveryService.Enum.WorkOrderStatus;
import com.KeyStone.DeliveryService.Repository.CustomerRepository;
import com.KeyStone.DeliveryService.Repository.UserRepository;
import com.KeyStone.DeliveryService.Repository.WorkOrderRepository;
import com.KeyStone.DeliveryService.Security.JWTUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private static final List<WorkOrderStatus> ACTIVE = List.of(
            WorkOrderStatus.ASSIGNED, WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.ON_HOLD);

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final WorkOrderRepository workOrderRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtil jwtUtil;
    private final JdbcTemplate jdbc;

    public UserService(UserRepository userRepository,
                       CustomerRepository customerRepository,
                       WorkOrderRepository workOrderRepository,
                       PasswordEncoder passwordEncoder,
                       JWTUtil jwtUtil,
                       JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.workOrderRepository = workOrderRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    // =========================================
    // AUTH
    // =========================================

    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO request) {
        User user = userRepository.findByEmailIgnoreCase(normalise(request.userEmail()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.info("Failed login for {}", user.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        }
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
        return new AuthResponseDTO(token, mapToResponse(user));
    }

    @Transactional(readOnly = true)
    public UserResponseDTO me(Integer callerId) {
        return mapToResponse(getOrThrow(callerId));
    }

    // =========================================
    // ADMIN (MANAGER)
    // =========================================

    @Transactional
    public UserResponseDTO createUser(UserRequestDTO request) {
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Password is required for a new user");
        }
        String email = normalise(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalStateException("An account with this email already exists");
        }
        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setRole(request.role());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setCustomer(resolveCustomer(request.role(), request.customerId()));
        User saved = userRepository.save(user);
        log.info("User {} ({}) created", saved.getId(), saved.getRole());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll(Sort.by("name")).stream().map(this::mapToResponse).toList();
    }

    /** Technicians with their current open-job count — used by dispatch to pick by load (F4). */
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getTechnicians() {
        return userRepository.findByRoleOrderByNameAsc(Role.TECHNICIAN).stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public long activeJobCount(Integer technicianId) {
        return workOrderRepository.countByTechnicianIdAndStatusIn(technicianId, ACTIVE);
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Integer id) {
        return mapToResponse(getOrThrow(id));
    }

    @Transactional
    public UserResponseDTO updateUser(Integer id, UserRequestDTO request) {
        User user = getOrThrow(id);
        String email = normalise(request.email());
        if (!email.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalStateException("An account with this email already exists");
        }
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setRole(request.role());
        user.setCustomer(resolveCustomer(request.role(), request.customerId()));
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        log.info("User {} updated", id);
        return mapToResponse(user);
    }

    @Transactional
    public void deleteUser(Integer callerId, Integer id) {
        if (id.equals(callerId)) {
            throw new IllegalStateException("You can't delete your own account");
        }
        User user = getOrThrow(id);
        if (user.getRole() == Role.TECHNICIAN && activeJobCount(id) > 0) {
            throw new IllegalStateException("Reassign this technician's open jobs before removing them");
        }
        // Detach the user from records that point at them, so the delete isn't blocked by
        // foreign keys. Work orders, history, parts and time entries stay — only the link to
        // this person is cleared. Their own notifications are removed.
        jdbc.update("DELETE FROM notifications WHERE recipient_id = ?", id);
        jdbc.update("UPDATE work_orders SET technician_id = NULL WHERE technician_id = ?", id);
        jdbc.update("UPDATE work_order_history SET changed_by_id = NULL WHERE changed_by_id = ?", id);
        jdbc.update("UPDATE work_order_parts SET logged_by_id = NULL WHERE logged_by_id = ?", id);
        jdbc.update("UPDATE work_order_times SET logged_by_id = NULL WHERE logged_by_id = ?", id);

        userRepository.delete(user);
        userRepository.flush();
        log.info("User {} deleted by {}", id, callerId);
    }

    // =========================================
    // HELPERS
    // =========================================

    private Customer resolveCustomer(Role role, Integer customerId) {
        if (role != Role.CUSTOMER) {
            return null;
        }
        if (customerId == null) {
            throw new IllegalArgumentException("Choose the organisation this customer login belongs to");
        }
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found: " + customerId));
    }

    private User getOrThrow(Integer id) {
        return userRepository.findById(id).orElseThrow(() -> new NoSuchElementException("User not found: " + id));
    }

    private static String normalise(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private UserResponseDTO mapToResponse(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt(),
                user.getCustomer() != null ? user.getCustomer().getId() : null);
    }
}
