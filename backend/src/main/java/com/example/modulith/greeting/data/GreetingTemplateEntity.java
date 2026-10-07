package com.example.modulith.greeting.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "greeting_template")
class GreetingTemplateEntity {

    @Id
    private Long id;

    @Column(nullable = false)
    private String template;

    @Column(name = "is_default", nullable = false)
    private boolean defaultTemplate;

    protected GreetingTemplateEntity() {
    }

    String getTemplate() {
        return template;
    }
}
