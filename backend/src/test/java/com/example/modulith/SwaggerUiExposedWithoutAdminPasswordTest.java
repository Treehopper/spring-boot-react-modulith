package com.example.modulith;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * If the management port listens beyond loopback and no {@code adminPassword} is set, Swagger UI is closed,
 * so exposing the port can't expose Swagger UI without a password by accident.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"management.server.port=0", "management.server.address=0.0.0.0"})
@ExtendWith(OutputCaptureExtension.class)
class SwaggerUiExposedWithoutAdminPasswordTest {

    @Value("${local.management.port}")
    int managementPort;

    @Test
    void swaggerUiIsClosedAndTheLogSaysSo(CapturedOutput output) throws Exception {
        try (HttpClient http = HttpClient.newHttpClient()) {
            HttpResponse<Void> response = http.send(
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + managementPort + "/actuator/swagger-ui/index.html"))
                            .header("Authorization", PortSeparationTest.basicAuth("admin"))
                            .build(),
                    HttpResponse.BodyHandlers.discarding());
            assertThat(response.statusCode()).isEqualTo(403);
        }
        assertThat(output.getAll()).contains("The management port listens beyond loopback (0.0.0.0) "
                + "and no adminPassword is set, so Swagger UI is closed");
    }
}
