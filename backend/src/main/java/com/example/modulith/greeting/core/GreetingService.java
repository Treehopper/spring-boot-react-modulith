package com.example.modulith.greeting.core;

import com.example.modulith.audience.api.AudienceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GreetingService {

    private static final Logger log = LoggerFactory.getLogger(GreetingService.class);

    private final GreetingTemplateStore templates;
    // Cross-component access goes through the audience component's api package only.
    private final AudienceApi audiences;

    GreetingService(GreetingTemplateStore templates, AudienceApi audiences) {
        this.templates = templates;
        this.audiences = audiences;
    }

    public Greeting greet() {
        String template = templates.findDefaultTemplate().orElse("Hello, %s!");
        String audience = audiences.defaultAudience().name();
        log.debug("Greeting audience '{}' with template '{}'", audience, template);
        return new Greeting(template.formatted(audience));
    }
}
