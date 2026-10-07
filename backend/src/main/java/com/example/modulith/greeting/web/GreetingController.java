package com.example.modulith.greeting.web;

import com.example.modulith.greeting.core.GreetingService;
import com.example.modulith.greeting.web.openapi.GreetingDto;
import com.example.modulith.greeting.web.openapi.GreetingsApi;
import org.springframework.web.bind.annotation.RestController;

/**
 * Implements the interface generated from {@code openapi/modulith-api.yaml}, so the
 * mappings and payloads cannot drift from the published contract.
 */
@RestController
class GreetingController implements GreetingsApi {

    private final GreetingService greetings;

    GreetingController(GreetingService greetings) {
        this.greetings = greetings;
    }

    @Override
    public GreetingDto getGreeting() {
        return new GreetingDto(greetings.greet().message());
    }
}
