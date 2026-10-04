package com.ridelink.account.service;

import com.ridelink.account.dto.UpdateProfileRequest;
import com.ridelink.account.dto.UserDTO;
import com.ridelink.account.entity.AccountStatus;
import com.ridelink.account.entity.User;
import com.ridelink.account.exception.BusinessException;
import com.ridelink.account.exception.ResourceNotFoundException;
import com.ridelink.account.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UserService — profile viewing, updating, and admin account management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final AuthService authService; // for toDTO mapping

    /**
     * Retrieves a paginated list of all users (Admin-only).
     */
    @Transactional(readOnly = true)
    public Page<UserDTO> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(authService::toDTO);
    }

    /**
     * Retrieves a single user by ID.
     */
    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        User user = findUserOrThrow(id);
        return authService.toDTO(user);
    }

    /**
     * Retrieves a user by email.
     */
    @Transactional(readOnly = true)
    public UserDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return authService.toDTO(user);
    }

    /**
     * Updates the authenticated user's own profile (partial update).
     * Only non-null fields in the request are applied.
     */
    public UserDTO updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUserOrThrow(userId);

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            // Ensure phone uniqueness
            if (!user.getPhone().equals(request.getPhone()) &&
                    userRepository.existsByPhone(request.getPhone())) {
                throw new BusinessException("Phone number already in use: " + request.getPhone());
            }
            user.setPhone(request.getPhone());
        }
        if (request.getProfilePictureUrl() != null) {
            user.setProfilePictureUrl(request.getProfilePictureUrl());
        }

        User updated = userRepository.save(user);
        log.info("User {} profile updated", userId);
        return authService.toDTO(updated);
    }

    /**
     * Admin: changes account status (ACTIVE / SUSPENDED / DELETED).
     */
    public UserDTO updateAccountStatus(Long userId, AccountStatus newStatus) {
        User user = findUserOrThrow(userId);

        if (user.getStatus() == AccountStatus.DELETED) {
            throw new BusinessException("Cannot update a deleted account.");
        }

        user.setStatus(newStatus);
        User updated = userRepository.save(user);
        log.info("Admin changed user {} status to {}", userId, newStatus);
        return authService.toDTO(updated);
    }

    // ─────────────── Helper ───────────────

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
