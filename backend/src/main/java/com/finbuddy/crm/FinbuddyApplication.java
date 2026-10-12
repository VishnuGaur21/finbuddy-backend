package com.finbuddy.crm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.modulith.Modulithic;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Finbuddy CRM - Enterprise Multi-Tenant Modular Monolith Application.
 * Powered by Spring Boot 3.3.x, Spring Modulith, and Java 21 LTS.
 */
@SpringBootApplication
@Modulithic(
        sharedModules = {"shared"},
        useFullyQualifiedModuleNames = false
)
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan
public class FinbuddyApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinbuddyApplication.class, args);
    }
}
