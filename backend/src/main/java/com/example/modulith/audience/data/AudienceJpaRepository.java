package com.example.modulith.audience.data;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface AudienceJpaRepository extends JpaRepository<AudienceEntity, Long> {

    Optional<AudienceEntity> findFirstByDefaultAudienceTrue();
}
