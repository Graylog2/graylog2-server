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

import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.OpenApiContextLocator;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.plugin.Version;

import java.util.Set;

/**
 * Configuration and setup for OpenAPI 3.1 specification generation.
 * This class encapsulates the logic for configuring Swagger/OpenAPI to work with Graylog's REST API.
 *
 * TODO: this class can't really be reused for multiple contexts right now, because it uses the global
 *   default context it. if used outside the usual application lifecycle, e.g. for stub generation or in
 *   tests, this is not ideal. Maybe we should expose the context and provide means to create it with a
 *   user-defined context id.
 */
// TODO: make the old generator work with the new annotations as an intermediate step
// TODO: make sure that the reader doesn't run into concurrency issues (call the openapi endpoint in quick succession. maybe we have to bind the resource as a singleton?)
@Singleton
public class OpenAPIGenerator {

    private final Version version;
    private final CustomOpenAPIScanner scanner;
    private final CustomModelConverter modelConverter;
    private final CustomReader.Factory readerFactory;

    @Inject
    public OpenAPIGenerator(Version version,
                            CustomOpenAPIScanner scanner,
                            CustomModelConverter modelConverter,
                            CustomReader.Factory readerFactory) {
        this.version = version;
        this.scanner = scanner;
        this.modelConverter = modelConverter;
        this.readerFactory = readerFactory;
    }

    // TODO: create a constructor/factory method to easily create a generator from a non-guice
    //   context? Probably need to provide Jackson subtypes as a parameter to register with
    //   the object mapper

    /**
     * Generates and returns the complete OpenAPI specification.
     * This is useful for testing or exporting the spec without running a server.
     *
     * @return The generated OpenAPI specification
     * @throws RuntimeException if the OpenAPI generation fails
     */
    public OpenAPI generateOpenApiSpec() {
        return openAPIContext().read();
    }

    public void ensureInitializedContext() {
        openAPIContext();
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

        return new SwaggerConfiguration()
                .openAPI31(true)
                .openAPI(openAPI)
                .prettyPrint(true)
                .sortOutput(true);
    }

    private OpenApiContext openAPIContext() {

        final var ctx = OpenApiContextLocator.getInstance().getOpenApiContext(OpenApiContext.OPENAPI_CONTEXT_ID_DEFAULT);

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
            return context.init();
        } catch (OpenApiConfigurationException e) {
            throw new RuntimeException("Unable to set up OpenAPI context" + e);
        }
    }


}
