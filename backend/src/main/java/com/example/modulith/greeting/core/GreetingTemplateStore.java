package com.example.modulith.greeting.core;

import java.util.Optional;

/**
 * Persistence port owned by the core layer and implemented by the data layer.
 */
public interface GreetingTemplateStore {

    /**
     * @return a {@link String#format} template with a single {@code %s} for the audience name
     */
    Optional<String> findDefaultTemplate();
}
