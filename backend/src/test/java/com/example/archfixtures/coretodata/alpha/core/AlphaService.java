package com.example.archfixtures.coretodata.alpha.core;

import com.example.archfixtures.coretodata.alpha.data.AlphaRow;

// Violation: core depends on the data layer instead of a port it owns.
public class AlphaService {
    AlphaRow row = new AlphaRow();
}
