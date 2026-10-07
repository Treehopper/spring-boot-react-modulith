package com.example.modulith;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end check of the hello-world slice: REST API through all layers and both components,
 * plus the embedded React build served from the classpath.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ModulithApplicationTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    void greetingFlowsThroughBothComponents() {
        assertThat(mvc.get().uri("/api/greetings"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.message").isEqualTo("Hello, World!");
    }

    @Test
    void servesEmbeddedFrontendAtRoot() {
        for (String path : new String[] {"/", "/index.html"}) {
            assertThat(mvc.get().uri(path))
                    .hasStatusOk()
                    .hasContentTypeCompatibleWith(MediaType.TEXT_HTML)
                    .bodyText().contains("<div id=\"root\"></div>");
        }
    }

    @Test
    void fallsBackToIndexHtmlForClientSideRoutes() {
        assertThat(mvc.get().uri("/some/client/route"))
                .hasStatusOk()
                .bodyText().contains("<div id=\"root\"></div>");
    }

    @Test
    void unknownApiEndpointsAndMissingAssetsStay404() {
        assertThat(mvc.get().uri("/api/unknown")).hasStatus(404);
        assertThat(mvc.get().uri("/assets/missing.js")).hasStatus(404);
    }
}
