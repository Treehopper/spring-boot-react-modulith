package com.example.modulith.audience.core;

import com.example.modulith.audience.api.Audience;

import java.util.Optional;

/**
 * Persistence port owned by the core layer and implemented by the data layer,
 * so the core never sees JPA.
 */
public interface AudienceStore {

    Optional<Audience> findDefault();
}
