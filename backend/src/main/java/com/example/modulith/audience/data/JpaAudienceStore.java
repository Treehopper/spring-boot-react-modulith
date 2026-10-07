package com.example.modulith.audience.data;

import com.example.modulith.audience.api.Audience;
import com.example.modulith.audience.core.AudienceStore;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class JpaAudienceStore implements AudienceStore {

    private final AudienceJpaRepository repository;

    JpaAudienceStore(AudienceJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Audience> findDefault() {
        return repository.findFirstByDefaultAudienceTrue().map(entity -> new Audience(entity.getName()));
    }
}
