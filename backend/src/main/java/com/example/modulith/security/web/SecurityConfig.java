package com.example.modulith.security.web;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Access to Swagger UI (and the contract it loads) on the management port. Everything else is open.
 * <ul>
 *     <li>{@code adminPassword} set: HTTP Basic login as {@value #ADMIN_USER}, for example
 *     {@code java -DadminPassword=... -jar modulith.jar}. A blank password fails startup.</li>
 *     <li>not set: open, without a login. The management port only listens on loopback
 *     ({@code management.server.address=127.0.0.1}), so only whoever is on this machine can reach it.</li>
 *     <li>not set, but the management port listens beyond loopback: closed, with a warning,
 *     so that exposing the port can't expose Swagger UI without a password by accident.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfig {

    static final String ADMIN_USER = "admin";

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    enum SwaggerUiAccess { LOGIN, OPEN, CLOSED }

    private final @Nullable String adminPassword;
    private final SwaggerUiAccess access;

    SecurityConfig(@Value("${adminPassword:#{null}}") @Nullable String adminPassword,
            @Value("${management.server.address:}") String managementAddress) {
        if (adminPassword != null && adminPassword.isBlank()) {
            throw new IllegalStateException("adminPassword must not be blank");
        }
        this.adminPassword = adminPassword;
        this.access = access(adminPassword != null, isLoopback(managementAddress));

        switch (access) {
            case OPEN -> log.info("Swagger UI is open without login (management port on {}). "
                    + "Set adminPassword to require one.", managementAddress);
            case CLOSED -> log.warn("The management port listens beyond loopback ({}) and no adminPassword is set, "
                    + "so Swagger UI is closed. Set -DadminPassword=... (before -jar), --adminPassword=... "
                    + "or the ADMINPASSWORD environment variable.",
                    managementAddress.isBlank() ? "all interfaces" : managementAddress);
            case LOGIN -> { }
        }
    }

    static SwaggerUiAccess access(boolean adminPasswordSet, boolean managementPortOnLoopback) {
        if (adminPasswordSet) {
            return SwaggerUiAccess.LOGIN;
        }
        return managementPortOnLoopback ? SwaggerUiAccess.OPEN : SwaggerUiAccess.CLOSED;
    }

    /** An unset address means all interfaces, which is not loopback. */
    static boolean isLoopback(@Nullable String address) {
        if (address == null || address.isBlank()) {
            return false;
        }
        try {
            return InetAddress.getByName(address).isLoopbackAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }

    @Bean
    @Order(1)
    SecurityFilterChain swaggerUiSecurity(HttpSecurity http) throws Exception {
        http.securityMatcher("/actuator/swagger-ui", "/actuator/swagger-ui/**", "/actuator/apicontract")
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        switch (access) {
            case LOGIN -> http.authorizeHttpRequests(requests -> requests.anyRequest().hasRole("ADMIN"))
                    .httpBasic(Customizer.withDefaults());
            case OPEN -> http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
            case CLOSED -> http.authorizeHttpRequests(requests -> requests.anyRequest().denyAll());
        }
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain openSecurity(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                // Nothing on this chain is authenticated, so there is no session a forged request could ride on.
                // Keeping CSRF on would only reject POST /actuator/loggers.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    /**
     * Always defined, so Spring Boot doesn't generate a user with a random password of its own.
     * Without a password there is no user.
     */
    @Bean
    UserDetailsService adminUser() {
        if (adminPassword == null) {
            return new InMemoryUserDetailsManager();
        }
        String encoded = PasswordEncoderFactories.createDelegatingPasswordEncoder().encode(adminPassword);
        return new InMemoryUserDetailsManager(User.withUsername(ADMIN_USER).password(encoded).roles("ADMIN").build());
    }
}
