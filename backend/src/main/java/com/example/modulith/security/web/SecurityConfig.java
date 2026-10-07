package com.example.modulith.security.web;

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

/**
 * Protects Swagger UI (and the contract it loads) on the management port with HTTP Basic:
 * user {@value #ADMIN_USER}, password from the {@code adminPassword} property, for example
 * {@code java -DadminPassword=... -jar modulith.jar}. Without a password, Swagger UI stays locked.
 * Everything else is open, as before.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfig {

    static final String ADMIN_USER = "admin";

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    @Order(1)
    SecurityFilterChain swaggerUiSecurity(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/actuator/swagger-ui", "/actuator/swagger-ui/**", "/actuator/apicontract")
                .authorizeHttpRequests(requests -> requests.anyRequest().hasRole("ADMIN"))
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
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

    @Bean
    UserDetailsService adminUser(@Value("${adminPassword:}") String adminPassword) {
        if (adminPassword.isBlank()) {
            log.warn("No adminPassword set, so Swagger UI is locked. Start with -DadminPassword=... (before -jar), "
                    + "--adminPassword=... or the ADMINPASSWORD environment variable.");
            return new InMemoryUserDetailsManager();
        }
        String encoded = PasswordEncoderFactories.createDelegatingPasswordEncoder().encode(adminPassword);
        return new InMemoryUserDetailsManager(User.withUsername(ADMIN_USER).password(encoded).roles("ADMIN").build());
    }
}
