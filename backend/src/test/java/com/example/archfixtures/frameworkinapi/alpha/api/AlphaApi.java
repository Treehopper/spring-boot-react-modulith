package com.example.archfixtures.frameworkinapi.alpha.api;

import org.springframework.util.MultiValueMap;

// Violation: the api contract exposes a framework type.
public interface AlphaApi {
    MultiValueMap<String, String> attributes();
}
