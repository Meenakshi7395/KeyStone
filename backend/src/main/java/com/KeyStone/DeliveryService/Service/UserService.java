package com.KeyStone.DeliveryService.Service;

import com.KeyStone.DeliveryService.DTO.AuthResponseDTO;
import com.KeyStone.DeliveryService.DTO.LoginRequestDTO;
import com.KeyStone.DeliveryService.DTO.RegisterRequestDTO;
import com.KeyStone.DeliveryService.DTO.UserResponseDTO;
import com.KeyStone.DeliveryService.Entity.Customer;
import com.KeyStone.DeliveryService.Entity.User;
import com.KeyStone.DeliveryService.Enum.Role;
import com.KeyStone.DeliveryService.Repository.CustomerRepository;
import com.KeyStone.DeliveryService.Repository.UserRepository;
import com.KeyStone.DeliveryService.Security.JWTUtil;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtil jwtUtil;

    public UserService(
            UserRepository userRepository,
            CustomerRepository customerRepository,
            PasswordEncoder passwordEncoder,
            JWTUtil jwtUtil) {

        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    // PUBLIC SELF-REGISTRATION — always CUSTOMER, never linked to an
    // organisation yet (see linkCustomer below).
    public UserResponseDTO registerCustomer(RegisterRequestDTO request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.CUSTOMER);

        return mapToResponse(userRepository.save(user));
    }

    // Self-service, one-time link from a CUSTOMER account to the
    // organisation (Customer) it represents. Once set it can only be
    // changed by a MANAGER (via PUT /api/users/{id}), which prevents a
    // rogue customer login from re-linking itself to someone else's
    // organisation after the fact to read their work orders.
    public UserResponseDTO linkCustomer(Integer callerId, Integer customerId) {

        User caller = userRepository.findById(callerId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + callerId));

        if (caller.getRole() != Role.CUSTOMER) {
            throw new IllegalStateException("Only customer accounts can be linked to an organisation");
        }

        if (caller.getCustomer() != null) {
            throw new IllegalStateException(
                    "This account is already linked — ask a manager to change it");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found: " + customerId));

        caller.setCustomer(customer);

        return mapToResponse(userRepository.save(caller));
    }

    // CREATE
    public UserResponseDTO createUser(User user) {

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Encrypt password before saving
        user.setPasswordHash(
                passwordEncoder.encode(user.getPasswordHash())
        );

        User savedUser = userRepository.save(user);

        return mapToResponse(savedUser);
    }

    // GET ALL
    public List<UserResponseDTO> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // GET BY ID
    public UserResponseDTO getUserById(Integer id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found with id: " + id
                        ));

        return mapToResponse(user);
    }

    // UPDATE
    public UserResponseDTO updateUser(Integer id, User user) {

        User existingUser = userRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found with id: " + id
                        ));

        existingUser.setName(user.getName());
        existingUser.setEmail(user.getEmail());
        existingUser.setRole(user.getRole());

        // Only update password if a new password is provided
        if (user.getPasswordHash() != null
                && !user.getPasswordHash().isBlank()) {

            existingUser.setPasswordHash(
                    passwordEncoder.encode(user.getPasswordHash())
            );
        }

        User updatedUser = userRepository.save(existingUser);

        return mapToResponse(updatedUser);
    }

    // DELETE
    public void deleteUser(Integer id) {

        if (!userRepository.existsById(id)) {
            throw new RuntimeException(
                    "User not found with id: " + id
            );
        }

        userRepository.deleteById(id);
    }

    // LOGIN + JWT
    public AuthResponseDTO login(LoginRequestDTO request) {

        User user = userRepository.findByEmail(request.userEmail())
                .orElseThrow(() ->
                        new BadCredentialsException(
                                "Invalid email or password"
                        ));

        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        );

        if (!passwordMatches) {
            throw new BadCredentialsException(
                    "Invalid email or password"
            );
        }

        // Generate JWT
        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole()
        );

        UserResponseDTO userResponse = mapToResponse(user);

        return new AuthResponseDTO(
                token,
                userResponse
        );
    }

    // ENTITY -> DTO
    private UserResponseDTO mapToResponse(User user) {

        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt(),
                user.getCustomer() != null ? user.getCustomer().getId() : null
        );
    }
}


