package com.example.modulith.security.web;

import com.example.modulith.security.web.SecurityConfig.SwaggerUiAccess;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class SecurityConfigTest {

    @Test
    void blankAdminPasswordFailsStartup() {
        assertThatIllegalStateException()
                .isThrownBy(() -> new SecurityConfig("  ", "127.0.0.1"))
                .withMessage("adminPassword must not be blank");
    }

    @Test
    void aPasswordMeansLoginOtherwiseOpenUnlessExposed() {
        assertThat(SecurityConfig.access(true, true)).isEqualTo(SwaggerUiAccess.LOGIN);
        assertThat(SecurityConfig.access(true, false)).isEqualTo(SwaggerUiAccess.LOGIN);
        assertThat(SecurityConfig.access(false, true)).isEqualTo(SwaggerUiAccess.OPEN);
        assertThat(SecurityConfig.access(false, false)).isEqualTo(SwaggerUiAccess.CLOSED);
    }

    @Test
    void recognisesLoopbackAddresses() {
        assertThat(SecurityConfig.isLoopback("127.0.0.1")).isTrue();
        assertThat(SecurityConfig.isLoopback("::1")).isTrue();
        assertThat(SecurityConfig.isLoopback("localhost")).isTrue();
        assertThat(SecurityConfig.isLoopback("0.0.0.0")).isFalse();
        assertThat(SecurityConfig.isLoopback("10.1.2.3")).isFalse();
        assertThat(SecurityConfig.isLoopback("")).isFalse();
        assertThat(SecurityConfig.isLoopback(null)).isFalse();
    }
}
