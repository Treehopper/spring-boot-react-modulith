package com.example.modulith.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves every rule in {@link ModulithRules} detects its violation, and only that one,
 * using the broken fixtures in {@code com.example.archfixtures}.
 */
class ModulithRulesTest {

    enum Rule {
        CLASSES_RESIDE_IN_COMPONENT_LAYERS(ModulithRules::classesResideInComponentLayers),
        LAYER_DEPENDENCIES(ModulithRules::layerDependencies),
        PERSISTENCE_ONLY_IN_DATA_LAYER(ModulithRules::persistenceOnlyInDataLayer),
        WEB_ONLY_IN_WEB_LAYER(ModulithRules::webOnlyInWebLayer),
        API_IS_PLAIN_JAVA(ModulithRules::apiIsPlainJava),
        COMPONENTS_ONLY_ACCESS_EACH_OTHER_THROUGH_API(ModulithRules::componentsOnlyAccessEachOtherThroughApi),
        COMPONENTS_ARE_FREE_OF_CYCLES(ModulithRules::componentsAreFreeOfCycles);

        private final Function<ModulithRules, ArchRule> definition;

        Rule(Function<ModulithRules, ArchRule> definition) {
            this.definition = definition;
        }

        boolean isViolatedBy(String root, JavaClasses classes) {
            // Fixtures are tiny, so some rules legitimately match no classes at all.
            return definition.apply(new ModulithRules(root)).allowEmptyShould(true).evaluate(classes).hasViolation();
        }
    }

    @ParameterizedTest(name = "{0} violates only {1}")
    @CsvSource({
            "straypackage,   CLASSES_RESIDE_IN_COMPONENT_LAYERS",
            "coretodata,     LAYER_DEPENDENCIES",
            "jpaincore,      PERSISTENCE_ONLY_IN_DATA_LAYER",
            "webindata,      WEB_ONLY_IN_WEB_LAYER",
            "frameworkinapi, API_IS_PLAIN_JAVA",
            "internalaccess, COMPONENTS_ONLY_ACCESS_EACH_OTHER_THROUGH_API",
            "cycle,          COMPONENTS_ARE_FREE_OF_CYCLES",
    })
    void fixtureViolatesExactlyTheExpectedRule(String scenario, Rule expected) {
        String root = "com.example.archfixtures." + scenario;
        JavaClasses classes = new ClassFileImporter().importPackages(root);
        assertThat(classes).as("fixture classes for %s", scenario).isNotEmpty();

        for (Rule rule : Rule.values()) {
            assertThat(rule.isViolatedBy(root, classes))
                    .as("%s violated by fixture '%s'", rule, scenario)
                    .isEqualTo(rule == expected);
        }
    }
}
