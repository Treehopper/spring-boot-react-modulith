package com.example.modulith;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Log levels can be changed through the actuator {@code loggers} endpoint on the management port,
 * and the change takes effect in the running application.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "management.server.port=0")
@ExtendWith(OutputCaptureExtension.class)
class LogLevelAtRuntimeTest {

    private static final String LOGGER = "com.example.modulith.greeting";

    private final HttpClient http = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    int serverPort;

    @Value("${local.management.port}")
    int managementPort;

    @Test
    void debugLoggingCanBeSwitchedOnWithoutRestart(CapturedOutput output) throws Exception {
        get(serverPort, "/api/greetings");
        assertThat(output).doesNotContain("Greeting audience 'World'");

        try {
            assertThat(setLevel(managementPort, "DEBUG").statusCode()).isEqualTo(204);
            assertThat(get(managementPort, "/actuator/loggers/" + LOGGER).body())
                    .contains("\"configuredLevel\":\"DEBUG\"");

            get(serverPort, "/api/greetings");
            assertThat(output).contains("Greeting audience 'World' with template 'Hello, %s!'");
        } finally {
            setLevel(managementPort, null);
        }
        assertThat(LoggerFactory.getLogger(LOGGER).isDebugEnabled()).isFalse();
    }

    @Test
    void loggersEndpointIsNotOnThePublicPort() throws Exception {
        assertThat(setLevel(serverPort, "DEBUG").statusCode()).isNotEqualTo(204);
        assertThat(LoggerFactory.getLogger(LOGGER).isDebugEnabled()).isFalse();
    }

    private HttpResponse<String> setLevel(int port, String level) throws IOException, InterruptedException {
        String body = level == null ? "{}" : "{\"configuredLevel\":\"" + level + "\"}";
        return http.send(HttpRequest.newBuilder(uri(port, "/actuator/loggers/" + LOGGER))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(int port, String path) throws IOException, InterruptedException {
        return http.send(HttpRequest.newBuilder(uri(port, path)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private static URI uri(int port, String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }
}
