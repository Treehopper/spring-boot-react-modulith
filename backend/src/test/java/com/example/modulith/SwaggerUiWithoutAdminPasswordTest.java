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
 * Without an {@code adminPassword}, Swagger UI on the loopback-only management port needs no login.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        // The extra property gives this class its own context, so its startup log is captured here.
        properties = {"management.server.port=0", "test.context=without-admin-password"})
@ExtendWith(OutputCaptureExtension.class)
class SwaggerUiWithoutAdminPasswordTest {

    @Value("${local.management.port}")
    int managementPort;

    @Test
    void swaggerUiAndContractAreOpen(CapturedOutput output) throws Exception {
        try (HttpClient http = HttpClient.newHttpClient()) {
            for (String path : new String[] {"/actuator/swagger-ui/index.html", "/actuator/apicontract"}) {
                HttpResponse<Void> response = http.send(
                        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + managementPort + path)).build(),
                        HttpResponse.BodyHandlers.discarding());
                assertThat(response.statusCode()).as(path).isEqualTo(200);
            }
        }
        assertThat(output.getAll()).contains("Swagger UI is open without login (management port on 127.0.0.1)");
    }
}
