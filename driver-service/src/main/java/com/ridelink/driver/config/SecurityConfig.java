package com.ridelink.driver.config;

import com.ridelink.driver.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Driver-service security configuration.
 * Only validates JWTs — does NOT issue tokens.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .headers(h -> h.frameOptions(f -> f.disable()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                                "/v3/api-docs/**", "/webjars/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // ride-service calls this without DRIVER role — allow for PASSENGER/DRIVER
                        .requestMatchers(HttpMethod.GET, "/api/drivers/available").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/drivers/by-profile/{id}").authenticated()
                        // Admin-only
                        .requestMatchers("/api/drivers/admin/**").hasRole("ADMIN")
                        // Driver-only
                        .requestMatchers(HttpMethod.POST, "/api/drivers/register").hasRole("DRIVER")
                        .requestMatchers(HttpMethod.POST, "/api/drivers/toggle-availability").hasRole("DRIVER")
                        .requestMatchers(HttpMethod.PUT, "/api/drivers/location").hasRole("DRIVER")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
