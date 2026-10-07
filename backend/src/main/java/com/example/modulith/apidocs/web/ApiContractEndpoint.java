package com.example.modulith.apidocs.web;

import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Serves the hand-written contract ({@code openapi/modulith-api.yaml}, copied onto the classpath by the build)
 * at {@code /actuator/apicontract} on the management port, where Swagger UI loads it.
 * Swagger UI shows the source of truth instead of a spec that springdoc derives from the code.
 */
@Component
@WebEndpoint(id = "apicontract")
class ApiContractEndpoint {

    private static final Resource CONTRACT = new ClassPathResource("openapi/modulith-api.yaml");

    @ReadOperation(produces = "application/yaml")
    Resource contract() {
        return CONTRACT;
    }
}
