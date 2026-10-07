package com.example.modulith.greeting.data;

import com.example.modulith.greeting.core.GreetingTemplateStore;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class JpaGreetingTemplateStore implements GreetingTemplateStore {

    private final GreetingTemplateJpaRepository repository;

    JpaGreetingTemplateStore(GreetingTemplateJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<String> findDefaultTemplate() {
        return repository.findFirstByDefaultTemplateTrue().map(GreetingTemplateEntity::getTemplate);
    }
}
