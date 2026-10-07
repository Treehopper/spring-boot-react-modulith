package com.example.modulith.audience.core;

import com.example.modulith.audience.api.Audience;
import com.example.modulith.audience.api.AudienceApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class AudienceService implements AudienceApi {

    private final AudienceStore store;

    AudienceService(AudienceStore store) {
        this.store = store;
    }

    @Override
    public Audience defaultAudience() {
        return store.findDefault().orElseThrow(() -> new IllegalStateException("No default audience configured"));
    }
}
