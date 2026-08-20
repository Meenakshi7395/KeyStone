package com.KeyStone.DeliveryService.Service;

import com.KeyStone.DeliveryService.DTO.AuthResponseDTO;
import com.KeyStone.DeliveryService.DTO.LoginRequestDTO;
import com.KeyStone.DeliveryService.DTO.UserResponseDTO;
import com.KeyStone.DeliveryService.Entity.User;
import com.KeyStone.DeliveryService.Repository.UserRepository;
import com.KeyStone.DeliveryService.Security.JWTUtil;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtil jwtUtil;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JWTUtil jwtUtil) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
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
                user.getCreatedAt()
        );
    }
}


