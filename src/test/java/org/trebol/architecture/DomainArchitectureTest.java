package org.trebol.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architecture tests to enforce Clean Architecture boundaries.
 * These tests ensure domain layer remains framework-independent.
 */
class DomainArchitectureTest {

    private final JavaClasses importedClasses = new ClassFileImporter()
        .importPackages("org.trebol");

    @Test
    void domainLayerShouldNotDependOnSpring() {
        ArchRule rule = noClasses()
            .that().resideInAnyPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..springframework..")
            .because("Domain layer must be framework-independent");

        rule.check(importedClasses);
    }

    @Test
    void domainLayerShouldNotDependOnJPA() {
        ArchRule rule = noClasses()
            .that().resideInAnyPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..jakarta.persistence..")
            .because("Domain layer must not depend on JPA");

        rule.check(importedClasses);
    }

    @Test
    void domainLayerShouldNotDependOnJakartaAnnotations() {
        ArchRule rule = noClasses()
            .that().resideInAnyPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..jakarta.validation..")
            .because("Domain layer should use its own validation");

        rule.check(importedClasses);
    }

    @Test
    void applicationLayerShouldNotDependOnAdapters() {
        ArchRule rule = noClasses()
            .that().resideInAnyPackage("org.trebol.application..")
            .and().haveSimpleNameNotEndingWith("Test")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.trebol.adapter..", "org.trebol.jpa..", "org.trebol.api..")
            .because("Application layer should only depend on domain and its own abstractions");

        rule.check(importedClasses);
    }
}
