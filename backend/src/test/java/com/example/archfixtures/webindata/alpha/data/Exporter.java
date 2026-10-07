package com.example.archfixtures.webindata.alpha.data;

import org.springframework.http.ResponseEntity;

// Violation: the data layer uses web types.
public class Exporter {
    public ResponseEntity<String> export() {
        return ResponseEntity.ok("data");
    }
}
