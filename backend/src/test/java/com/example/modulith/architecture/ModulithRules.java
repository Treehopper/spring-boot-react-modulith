package com.example.modulith.architecture;

import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Architecture rules for a modulith laid out as {@code <root>.<component>.<layer>}, where layer is one of
 * <ul>
 *     <li>{@code api}: the component's published contract, the only thing other components may use</li>
 *     <li>{@code core}: business logic, plus the persistence ports the data layer implements</li>
 *     <li>{@code data}: persistence adapters (JPA)</li>
 *     <li>{@code web}: HTTP adapters (Spring MVC)</li>
 * </ul>
 * The rules take the root package as a parameter so they can be verified against broken fixtures.
 */
public final class ModulithRules {

    private static final String[] PERSISTENCE_PACKAGES = {
            "jakarta.persistence..", "org.hibernate..", "org.springframework.data..",
            "org.springframework.orm..", "org.springframework.jdbc.."};

    private static final String[] WEB_PACKAGES = {
            "org.springframework.web..", "org.springframework.http..", "org.springframework.ui..",
            "org.springframework.boot.web..", "org.springframework.boot.webmvc..",
            "org.springframework.data.web..", "jakarta.servlet.."};

    private final String root;

    public ModulithRules(String root) {
        this.root = root;
    }

    private String layer(String name) {
        return root + ".*." + name + "..";
    }

    /** Every class below the root lives in {@code <root>.<component>.(api|core|data|web)}. */
    public ArchRule classesResideInComponentLayers() {
        return classes().that().resideInAPackage(root + "..").and().resideOutsideOfPackage(root)
                .should().resideInAnyPackage(layer("api"), layer("core"), layer("data"), layer("web"))
                .because("code is organised as <root>.<component>.(api|core|data|web)");
    }

    /** web -> core -> api, data -> core (dependency inversion); nothing depends on web or data. */
    public ArchRule layerDependencies() {
        return layeredArchitecture().consideringOnlyDependenciesInLayers()
                .withOptionalLayers(true)
                .layer("API").definedBy(layer("api"))
                .layer("Core").definedBy(layer("core"))
                .layer("Data").definedBy(layer("data"))
                .layer("Web").definedBy(layer("web"))
                .whereLayer("Web").mayNotBeAccessedByAnyLayer()
                .whereLayer("Data").mayNotBeAccessedByAnyLayer()
                .whereLayer("Core").mayOnlyBeAccessedByLayers("Web", "Data")
                .whereLayer("API").mayOnlyBeAccessedByLayers("Core", "Web", "Data");
    }

    /** JPA and other persistence technology must not leak out of the data layer. */
    public ArchRule persistenceOnlyInDataLayer() {
        return noClasses().that().resideInAnyPackage(layer("api"), layer("core"), layer("web"))
                .should().dependOnClassesThat().resideInAnyPackage(PERSISTENCE_PACKAGES)
                .because("only the data layer may use persistence technology; core talks to it through ports");
    }

    /** Web technology must not leak out of the web layer. */
    public ArchRule webOnlyInWebLayer() {
        return noClasses().that().resideInAnyPackage(layer("api"), layer("core"), layer("data"))
                .should().dependOnClassesThat().resideInAnyPackage(WEB_PACKAGES)
                .because("only the web layer may use web technology");
    }

    /** A component's api is a plain Java contract that does not drag in frameworks or internals. */
    public ArchRule apiIsPlainJava() {
        return classes().that().resideInAPackage(layer("api"))
                .should().onlyDependOnClassesThat().resideInAnyPackage("java..", "org.jspecify..", layer("api"))
                .because("an api package is a framework-free contract between components");
    }

    /** Components only use each other's {@code api} package. */
    public ArchRule componentsOnlyAccessEachOtherThroughApi() {
        return slices().matching(root + ".(*)..").namingSlices("component '$1'")
                .should().notDependOnEachOther()
                .ignoreDependency(alwaysTrue(), resideInAPackage(layer("api")))
                .because("components may only access one another through their api package");
    }

    public ArchRule componentsAreFreeOfCycles() {
        return slices().matching(root + ".(*)..").namingSlices("component '$1'")
                .should().beFreeOfCycles();
    }
}
