package com.ridelink.account.service;

import com.ridelink.account.dto.*;
import com.ridelink.account.entity.AccountStatus;
import com.ridelink.account.entity.Role;
import com.ridelink.account.entity.User;
import com.ridelink.account.exception.BusinessException;
import com.ridelink.account.exception.ResourceNotFoundException;
import com.ridelink.account.repository.UserRepository;
import com.ridelink.account.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication service — handles registration and login.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    /**
     * Registers a new user.
     * Validates uniqueness of email and phone, hashes the password, and saves to DB.
     *
     * @param request registration payload
     * @return the persisted user as DTO
     * @throws BusinessException if email or phone already exists, or role is ADMIN
     */
    public UserDTO register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        // Prevent self-registration as ADMIN
        if (request.getRole() == Role.ADMIN) {
            throw new BusinessException("Self-registration as ADMIN is not allowed.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("An account already exists with email: " + request.getEmail());
        }

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException("An account already exists with phone: " + request.getPhone());
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(request.getRole())
                .status(AccountStatus.ACTIVE)
                .build();

        User saved = userRepository.save(user);
        log.info("User registered successfully with id: {}", saved.getId());
        return toDTO(saved);
    }

    /**
     * Authenticates a user and issues a JWT.
     *
     * @param request login credentials
     * @return AuthResponse containing the JWT and user info
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        // Spring Security will throw BadCredentialsException if invalid
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", -1L));

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("Account is " + user.getStatus().name().toLowerCase() +
                    ". Please contact support.");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails, user.getId(), user.getRole().name());

        log.info("User {} logged in successfully", user.getId());

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getJwtExpirationMs())
                .user(toDTO(user))
                .build();
    }

    // ─────────────────── Mapper ───────────────────

    public UserDTO toDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .profilePictureUrl(user.getProfilePictureUrl())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
