package com.ridelink.account.service;

import com.ridelink.account.dto.LoginRequest;
import com.ridelink.account.dto.RegisterRequest;
import com.ridelink.account.dto.UserDTO;
import com.ridelink.account.entity.AccountStatus;
import com.ridelink.account.entity.Role;
import com.ridelink.account.entity.User;
import com.ridelink.account.exception.BusinessException;
import com.ridelink.account.repository.UserRepository;
import com.ridelink.account.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService.
 * Tests happy paths, edge cases, and failure scenarios.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRegisterRequest;
    private User savedUser;

    @BeforeEach
    void setUp() {
        validRegisterRequest = new RegisterRequest();
        validRegisterRequest.setFullName("Sahan Perera");
        validRegisterRequest.setEmail("sahan@example.com");
        validRegisterRequest.setPassword("SecurePass123!");
        validRegisterRequest.setPhone("+94771234567");
        validRegisterRequest.setRole(Role.PASSENGER);

        savedUser = User.builder()
                .id(1L)
                .fullName("Sahan Perera")
                .email("sahan@example.com")
                .passwordHash("$2a$hashed")
                .phone("+94771234567")
                .role(Role.PASSENGER)
                .status(AccountStatus.ACTIVE)
                .build();
    }

    // ───────────────── Registration Tests ─────────────────

    @Nested
    @DisplayName("Registration")
    class RegistrationTests {

        @Test
        @DisplayName("Happy Path: Should register a new PASSENGER successfully")
        void register_happyPath_passenger() {
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByPhone(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$hashed");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            UserDTO result = authService.register(validRegisterRequest);

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("sahan@example.com");
            assertThat(result.getRole()).isEqualTo(Role.PASSENGER);
            assertThat(result.getStatus()).isEqualTo(AccountStatus.ACTIVE);
            verify(passwordEncoder).encode("SecurePass123!");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Failure: Should throw BusinessException for duplicate email")
        void register_duplicateEmail_throwsBusinessException() {
            when(userRepository.existsByEmail("sahan@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(validRegisterRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already exists with email");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Failure: Should throw BusinessException for duplicate phone")
        void register_duplicatePhone_throwsBusinessException() {
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByPhone("+94771234567")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(validRegisterRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already exists with phone");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Failure: Should prevent self-registration as ADMIN")
        void register_adminRole_throwsBusinessException() {
            validRegisterRequest.setRole(Role.ADMIN);

            assertThatThrownBy(() -> authService.register(validRegisterRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("ADMIN");

            verify(userRepository, never()).existsByEmail(anyString());
        }
    }

    // ───────────────── Login Tests ─────────────────

    @Nested
    @DisplayName("Login")
    class LoginTests {

        private LoginRequest loginRequest;

        @BeforeEach
        void setUp() {
            loginRequest = new LoginRequest();
            loginRequest.setEmail("sahan@example.com");
            loginRequest.setPassword("SecurePass123!");
        }

        @Test
        @DisplayName("Happy Path: Should login and return JWT")
        void login_happyPath_returnsJwt() {
            when(userRepository.findByEmail("sahan@example.com")).thenReturn(Optional.of(savedUser));
            UserDetails mockDetails = mock(UserDetails.class);
            when(userDetailsService.loadUserByUsername("sahan@example.com")).thenReturn(mockDetails);
            when(jwtUtil.generateToken(any(), eq(1L), eq("PASSENGER"))).thenReturn("jwt.token.here");
            when(jwtUtil.getJwtExpirationMs()).thenReturn(86400000L);

            var response = authService.login(loginRequest);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("jwt.token.here");
            assertThat(response.getTokenType()).isEqualTo("Bearer");
            assertThat(response.getUser().getEmail()).isEqualTo("sahan@example.com");
        }

        @Test
        @DisplayName("Failure: Should throw BadCredentialsException for wrong password")
        void login_wrongPassword_throwsBadCredentials() {
            doThrow(new BadCredentialsException("Bad credentials"))
                    .when(authenticationManager).authenticate(any());

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("Failure: Should throw BusinessException for suspended account")
        void login_suspendedAccount_throwsBusinessException() {
            savedUser.setStatus(AccountStatus.SUSPENDED);
            when(userRepository.findByEmail("sahan@example.com")).thenReturn(Optional.of(savedUser));

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("suspended");
        }
    }
}
