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

import com.fasterxml.jackson.databind.SerializationFeature;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.core.util.PrimitiveType;
import io.swagger.v3.core.util.Yaml31;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.OpenApiContextLocator;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.plugin.Version;

import java.util.Set;

/**
 * Configuration and setup for OpenAPI 3.1 description generation.
 * This class encapsulates the logic for configuring Swagger/OpenAPI to work with Graylog's REST API.
 */
@Singleton
public class OpenAPIContextFactory {

    static {
        // Register custom class mappings for simple types that should be treated as primitives.
        // Joda-Time types
        PrimitiveType.customClasses().put("org.joda.time.DateTimeZone", PrimitiveType.STRING);
        PrimitiveType.customClasses().put("org.joda.time.Duration", PrimitiveType.LONG);
        PrimitiveType.customClasses().put("org.joda.time.Period", PrimitiveType.STRING);
        // java.time types (serialized as ISO-8601 strings by JavaTimeModule)
        PrimitiveType.customClasses().put("java.time.Duration", PrimitiveType.STRING);
        PrimitiveType.customClasses().put("java.time.ZoneId", PrimitiveType.STRING);
        PrimitiveType.customClasses().put("java.time.ZoneOffset", PrimitiveType.STRING);
        // Third-party types (with custom Jackson serializers)
        PrimitiveType.customClasses().put("org.threeten.extra.PeriodDuration", PrimitiveType.STRING);
        PrimitiveType.customClasses().put("com.github.zafarkhaja.semver.Version", PrimitiveType.STRING);
        PrimitiveType.customClasses().put("com.google.common.net.HostAndPort", PrimitiveType.STRING);
        PrimitiveType.customClasses().put("com.github.joschi.jadconfig.util.Size", PrimitiveType.LONG);
        PrimitiveType.customClasses().put("org.bson.types.ObjectId", PrimitiveType.STRING);
        PrimitiveType.customClasses().put("org.apache.shiro.authz.permission.WildcardPermission", PrimitiveType.STRING);
        PrimitiveType.customClasses().put("com.vdurmont.semver4j.Semver", PrimitiveType.STRING);
        PrimitiveType.customClasses().put("com.vdurmont.semver4j.Requirement", PrimitiveType.STRING);
        // Graylog internal types (with custom Jackson serializers)
        PrimitiveType.customClasses().put("org.graylog.grn.GRN", PrimitiveType.STRING);
    }

    private final Version version;
    private final CustomOpenAPIScanner scanner;
    private final CustomModelConverter modelConverter;
    private final CustomReader.Factory readerFactory;

    @Inject
    public OpenAPIContextFactory(Version version,
                                 CustomOpenAPIScanner scanner,
                                 CustomModelConverter modelConverter,
                                 CustomReader.Factory readerFactory) {
        this.version = version;
        this.scanner = scanner;
        this.modelConverter = modelConverter;
        this.readerFactory = readerFactory;
    }

    /**
     * Retrieves an existing OpenAPI context by its ID or creates a new one and registers it under the given ID if
     * it doesn't exist.
     * <p>
     * <b>
     * Contexts are cached globally, so even if you create a completely new {@link OpenAPIContextFactory}
     * object but reuuse a context-id that was previously used with a different factory object, you will get back the
     * cached version, which can be confusing. So make sure to use a unique context-id if you don't want this behavior.
     * </b>>
     *
     * @param contextId The ID of the OpenAPI context to retrieve or create.
     * @return The existing or newly created OpenAPI context.
     */
    public OpenApiContext getOrCreate(String contextId) {

        final var ctx = OpenApiContextLocator.getInstance().getOpenApiContext(contextId);

        if (ctx != null) {
            return ctx;
        }

        final var openApiConfiguration = swaggerConfig();

        try {
            final var context = new JaxrsOpenApiContextBuilder<>()
                    .openApiConfiguration(openApiConfiguration)
                    .buildContext(false);
            context.setModelConverters(Set.of(modelConverter));
            context.setOpenApiScanner(scanner);
            context.setOpenApiReader(readerFactory.create(openApiConfiguration));
            context.setOutputJsonMapper(Json31.mapper().copy().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS));
            context.setOutputYamlMapper(Yaml31.mapper().copy().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS));
            return context.init();
        } catch (OpenApiConfigurationException e) {
            throw new RuntimeException("Unable to set up OpenAPI context" + e);
        }
    }

    private SwaggerConfiguration swaggerConfig() {
        final var info = new Info()
                .title("REST API")
                .version(version.toString())
                .description("The REST API provides programmatic access for automating functions or for integrating " +
                        "with other systems. The REST API allows you to perform any actions that you can " +
                        "perform through the web interface.")
                .license(new License().name("SSPLv1").url("https://www.mongodb.com/licensing/server-side-public-license"));

        final var securitySchemes = new Components()
                .addSecuritySchemes("tokenAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("basic")
                        .description("API Token Authentication - use your API token as username and literal 'token' as password"))
                .addSecuritySchemes("basicAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("basic")
                        .description("HTTP Basic Authentication with username and password"))
                .addSecuritySchemes("sessionAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name("authentication")
                        .description("Session Cookie Authentication - used by the web interface after logging in"));

        final var openAPI = new OpenAPI()
                .info(info)
                .components(securitySchemes)
                .addServersItem(
                        new Server()
                                .description("REST API endpoint that is also used by the web interface.")
                                .url("/api/"))
                .addSecurityItem(new SecurityRequirement().addList("tokenAuth"))
                .addSecurityItem(new SecurityRequirement().addList("basicAuth"))
                .addSecurityItem(new SecurityRequirement().addList("sessionAuth"));

        return new SwaggerConfiguration()
                .openAPI31(true)
                .openAPI(openAPI)
                .prettyPrint(true)
                .sortOutput(true);
    }
}
