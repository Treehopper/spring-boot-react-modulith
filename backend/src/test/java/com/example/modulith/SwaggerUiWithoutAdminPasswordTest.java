package com.example.modulith;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Without an {@code adminPassword}, Swagger UI is locked rather than falling back to a default password.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "management.server.port=0")
class SwaggerUiWithoutAdminPasswordTest {

    @Value("${local.management.port}")
    int managementPort;

    @Test
    void swaggerUiIsLocked() throws Exception {
        try (HttpClient http = HttpClient.newHttpClient()) {
            for (String password : new String[] {"", "admin", "password"}) {
                HttpResponse<Void> response = http.send(
                        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + managementPort + "/actuator/swagger-ui"))
                                .header("Authorization", PortSeparationTest.basicAuth(password))
                                .build(),
                        HttpResponse.BodyHandlers.discarding());
                assertThat(response.statusCode()).as("password '%s'", password).isEqualTo(401);
            }
        }
    }
}
