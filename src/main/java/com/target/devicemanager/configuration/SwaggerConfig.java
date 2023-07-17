package com.target.devicemanager.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration

public class SwaggerConfig {

    @Bean
    public OpenAPI api() {
        return new OpenAPI()
                .components(new Components())
                .info(new Info().title("Posssum")
                        .description("Spring Boot API used to present the functionality of all USB-connected POS peripherals")
                        .contact(new Contact()
                                .name("POSSUM Team")));
    }

    @Bean
    public GroupedOpenApi defaultApi() {
        return GroupedOpenApi.builder()
                .group("Default")
                .packagesToScan("com.target.devicemanager")
                .pathsToExclude("/*/simulate/**")
                .build();
    }

    @Bean
    public GroupedOpenApi simulatorApi() {
        return GroupedOpenApi.builder()
                .group("Simulator Controls")
                .packagesToScan("com.target.devicemanager")
                .pathsToMatch("/*/simulate/**")
                .build();
    }
}