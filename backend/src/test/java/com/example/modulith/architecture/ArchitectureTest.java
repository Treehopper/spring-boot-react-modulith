package com.example.modulith.architecture;

import com.example.modulith.ModulithApplication;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Enforces {@link ModulithRules} on the production code.
 */
@AnalyzeClasses(packagesOf = ModulithApplication.class, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final ModulithRules RULES = new ModulithRules(ModulithApplication.class.getPackageName());

    @ArchTest
    static final ArchRule classesResideInComponentLayers = RULES.classesResideInComponentLayers();

    @ArchTest
    static final ArchRule layerDependencies = RULES.layerDependencies();

    @ArchTest
    static final ArchRule persistenceOnlyInDataLayer = RULES.persistenceOnlyInDataLayer();

    @ArchTest
    static final ArchRule webOnlyInWebLayer = RULES.webOnlyInWebLayer();

    @ArchTest
    static final ArchRule apiIsPlainJava = RULES.apiIsPlainJava();

    @ArchTest
    static final ArchRule componentsOnlyAccessEachOtherThroughApi = RULES.componentsOnlyAccessEachOtherThroughApi();

    @ArchTest
    static final ArchRule componentsAreFreeOfCycles = RULES.componentsAreFreeOfCycles();
}
