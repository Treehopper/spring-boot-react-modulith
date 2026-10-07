package com.example.archfixtures.internalaccess.beta.core;

import com.example.archfixtures.internalaccess.alpha.core.AlphaService;

// Violation: beta reaches into alpha's core instead of alpha's api.
public class BetaService {
    String greet(AlphaService alpha) {
        return alpha.hello();
    }
}
