package com.example.archfixtures.jpaincore.alpha.core;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

// Violation: a JPA entity in the core layer.
@Entity
public class Thing {
    @Id
    Long id;
}
