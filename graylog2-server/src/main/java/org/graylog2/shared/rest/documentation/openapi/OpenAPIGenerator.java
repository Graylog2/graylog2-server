/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.shared.rest.documentation.openapi;

import io.swagger.v3.jaxrs2.integration.JaxrsAnnotationScanner;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.jaxrs2.integration.resources.BaseOpenApiResource;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.graylog2.plugin.Version;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.bindings.providers.config.ObjectMapperConfiguration;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Configuration and setup for OpenAPI 3.1 specification generation.
 * This class encapsulates the logic for configuring Swagger/OpenAPI to work with Graylog's REST API.
 */
@Singleton
public class OpenAPIGenerator {

    private final Set<Class<?>> resourceClasses;
    private final Version version;

    @Inject
    public OpenAPIGenerator(
            @Named(Graylog2Module.SYSTEM_REST_RESOURCES) final Set<Class<?>> systemRestResources,
            final Map<String, Set<Class<? extends PluginRestResource>>> pluginRestResources,
            Version version,
            ObjectMapperConfiguration objectMapperConfiguration) {

        this.resourceClasses = Stream.concat(systemRestResources.stream(),
                        pluginRestResources.values().stream().flatMap(Set::stream))
                .collect(Collectors.toSet());
        this.version = version;

        // Globally register our custom object mapper configuration
        if (!ObjectMapperConfigurer.isInitialized()) {
            ObjectMapperConfigurer.initialize(objectMapperConfiguration);
        }
    }

    /**
     * Builds and returns a configured OpenApiResource that can be registered with Jersey.
     * This is the main entry point for production use.
     *
     * @return A configured BaseOpenApiResource ready to be registered with Jersey
     */
    public BaseOpenApiResource openAPIResource() {
        return new OpenApiResource().openApiConfiguration(swaggerConfig());
    }

    /**
     * Generates and returns the complete OpenAPI specification.
     * This is useful for testing or exporting the spec without running a server.
     *
     * @return The generated OpenAPI specification
     * @throws RuntimeException if the OpenAPI generation fails
     */
    public OpenAPI generateOpenApiSpec() {
        try {
            // Build the OpenAPI context which triggers schema generation
            var context = new JaxrsOpenApiContextBuilder<>()
                    .openApiConfiguration(swaggerConfig())
                    .buildContext(true);

            return context.read();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate OpenAPI specification", e);
        }
    }

    private SwaggerConfiguration swaggerConfig() {
        final var info = new Info()
                .title("Graylog REST API")
                .version(version.toString())
                .description("""
                        The Graylog REST API provides programmatic access to Graylog for automating functions
                        or for integrating with other systems. The REST API allows you to perform any actions
                        that you can perform through the Graylog web interface.
                        """)
                .contact(new Contact().name("Graylog").url("https://www.graylog.com"))
                .license(new License().name("SSPLv1").url("https://www.mongodb.com/licensing/server-side-public-license"));

        final var openAPI = new OpenAPI()
                .info(info);
        // TODO: add server and security spec

        // TODO: We have to filter the resources based on certain criteria (e.g. CLOUD_VISIBLE).
        //   We might want to do this with a custom io.swagger.v3.jaxrs2.integration.JaxrsAnnotationScanner
        //   implementation, or with a custom io.swagger.v3.core.filter.OpenAPISpecFilter, if possible

        final var resourceClassNames = resourceClasses.stream()
                .map(Class::getName)
                .collect(Collectors.toSet());

        return new SwaggerConfiguration()
                .openAPI31(true)
                .openAPI(openAPI)
                .prettyPrint(true)
                .sortOutput(true)
                .scannerClass(JaxrsAnnotationScanner.class.getName())
                .objectMapperProcessorClass(CustomObjectMapperProcessor.class.getName())
                .modelConverterClasses(Set.of(CustomModelConverter.class.getName()))
                .resourceClasses(resourceClassNames);
    }

}
