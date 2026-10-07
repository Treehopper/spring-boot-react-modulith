package com.example.archfixtures.cycle.alpha.api;

import com.example.archfixtures.cycle.beta.api.BetaDto;

// Violation (together with BetaDto): alpha and beta depend on each other.
public interface AlphaApi {
    BetaDto beta();
}
