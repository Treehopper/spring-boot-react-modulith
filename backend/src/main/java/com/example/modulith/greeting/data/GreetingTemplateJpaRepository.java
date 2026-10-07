package com.example.modulith.greeting.data;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface GreetingTemplateJpaRepository extends JpaRepository<GreetingTemplateEntity, Long> {

    Optional<GreetingTemplateEntity> findFirstByDefaultTemplateTrue();
}
