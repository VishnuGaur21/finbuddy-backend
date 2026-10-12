package com.finbuddy.crm;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Spring Modulith Architecture Verification Test.
 * Validates module boundaries, prevents cyclic dependencies, and verifies loose coupling.
 */
class ModularityTests {

    private final ApplicationModules modules = ApplicationModules.of(FinbuddyApplication.class);

    @Test
    void verifyModularity() {
        // Enforces strict architectural rules across all 15 modules
        modules.verify();
    }

    @Test
    void createModuleDocumentation() {
        // Generates C4 / PlantUML architectural documentation automatically
        new Documenter(modules).writeDocumentation();
    }
}
