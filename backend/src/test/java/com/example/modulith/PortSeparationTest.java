package com.example.modulith;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Against a real server (unlike MockMvc, this includes the servlet container's error dispatch):
 * the frontend and API live on the public port, Swagger UI on the management port behind HTTP Basic,
 * and neither leaks onto the other.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"management.server.port=0", "adminPassword=" + PortSeparationTest.ADMIN_PASSWORD})
class PortSeparationTest {

    static final String ADMIN_PASSWORD = "test-secret";

    private final HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

    @Value("${local.server.port}")
    int serverPort;

    @Value("${local.management.port}")
    int managementPort;

    @Test
    void publicPortServesFrontendAndApi() throws Exception {
        assertThat(get(serverPort, "/").body()).contains("<div id=\"root\"></div>");
        assertThat(get(serverPort, "/some/client/route").body()).contains("<div id=\"root\"></div>");
        assertThat(get(serverPort, "/api/greetings").body()).contains("Hello, World!");
    }

    @Test
    void publicPortKeeps404sFor404s() throws Exception {
        assertThat(get(serverPort, "/api/unknown").statusCode()).isEqualTo(404);
        assertThat(get(serverPort, "/assets/missing.js").statusCode()).isEqualTo(404);
    }

    @Test
    void hashedAssetsAreCachedForGoodWhileIndexHtmlIsRevalidated() throws Exception {
        HttpResponse<String> index = get(serverPort, "/");
        Matcher bundle = Pattern.compile("/assets/[^\"]+\\.js").matcher(index.body());
        assertThat(bundle.find()).as("index.html references a hashed bundle").isTrue();

        assertThat(get(serverPort, bundle.group()).headers().allValues("Cache-Control"))
                .containsExactly("max-age=31536000, public, immutable");
        for (String path : new String[] {"/", "/index.html", "/some/client/route", "/favicon.svg"}) {
            assertThat(get(serverPort, path).headers().allValues("Cache-Control")).as(path).containsExactly("no-cache");
            assertThat(head(serverPort, path).statusCode()).as("HEAD %s", path).isEqualTo(200);
        }
    }

    @Test
    void managementPortServesSwaggerUiWithTheHandWrittenContract() throws Exception {
        HttpResponse<String> swaggerUi = get(managementPort, "/actuator/swagger-ui", ADMIN_PASSWORD);
        assertThat(swaggerUi.statusCode()).isEqualTo(200);
        assertThat(swaggerUi.body()).contains("swagger-ui");

        assertThat(get(managementPort, "/actuator/swagger-ui/swagger-config", ADMIN_PASSWORD).body())
                .contains("\"url\":\"/actuator/apicontract\"")
                .contains("\"supportedSubmitMethods\":[]");

        HttpResponse<String> contract = get(managementPort, "/actuator/apicontract", ADMIN_PASSWORD);
        assertThat(contract.headers().firstValue("Content-Type")).hasValueSatisfying(
                type -> assertThat(type).startsWith("application/yaml"));
        assertThat(contract.body()).contains("operationId: getGreeting");
    }

    @Test
    void swaggerUiAndContractRequireTheAdminPassword() throws Exception {
        for (String path : new String[] {"/actuator/swagger-ui", "/actuator/swagger-ui/index.html",
                "/actuator/swagger-ui/swagger-config", "/actuator/apicontract"}) {
            HttpResponse<String> anonymous = get(managementPort, path);
            assertThat(anonymous.statusCode()).as("anonymous %s", path).isEqualTo(401);
            assertThat(anonymous.headers().firstValue("WWW-Authenticate")).hasValueSatisfying(
                    challenge -> assertThat(challenge).startsWith("Basic"));
            assertThat(get(managementPort, path, "wrong").statusCode()).as("wrong password %s", path).isEqualTo(401);
        }
    }

    @Test
    void everythingElseStaysOpen() throws Exception {
        assertThat(get(managementPort, "/actuator/health").statusCode()).isEqualTo(200);
        assertThat(get(managementPort, "/actuator/loggers").statusCode()).isEqualTo(200);
        assertThat(get(serverPort, "/api/greetings").statusCode()).isEqualTo(200);
        assertThat(get(serverPort, "/").statusCode()).isEqualTo(200);
    }

    @Test
    void codeDerivedSpecIsNotExposed() throws Exception {
        assertThat(get(managementPort, "/actuator/openapi").statusCode()).isEqualTo(404);
        assertThat(get(managementPort, "/v3/api-docs").statusCode()).isEqualTo(404);
    }

    @Test
    void portsDoNotLeakIntoEachOther() throws Exception {
        assertThat(get(managementPort, "/").statusCode()).isEqualTo(404);
        assertThat(get(managementPort, "/some/client/route").statusCode()).isEqualTo(404);
        assertThat(get(serverPort, "/actuator/swagger-ui", ADMIN_PASSWORD).body()).doesNotContain("swagger-ui");
        assertThat(get(serverPort, "/v3/api-docs").body()).doesNotContain("openapi");
    }

    private HttpResponse<String> get(int port, String path) throws IOException, InterruptedException {
        return http.send(HttpRequest.newBuilder(uri(port, path)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<Void> head(int port, String path) throws IOException, InterruptedException {
        return http.send(HttpRequest.newBuilder(uri(port, path)).HEAD().build(), HttpResponse.BodyHandlers.discarding());
    }

    private HttpResponse<String> get(int port, String path, String adminPassword) throws IOException, InterruptedException {
        return http.send(HttpRequest.newBuilder(uri(port, path))
                        .header("Authorization", basicAuth(adminPassword))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    static String basicAuth(String password) {
        return "Basic " + Base64.getEncoder().encodeToString(("admin:" + password).getBytes(StandardCharsets.UTF_8));
    }

    private static URI uri(int port, String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }
}
