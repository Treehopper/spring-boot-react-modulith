package com.example.modulith.audience.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "audience")
class AudienceEntity {

    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "is_default", nullable = false)
    private boolean defaultAudience;

    protected AudienceEntity() {
    }

    String getName() {
        return name;
    }
}
