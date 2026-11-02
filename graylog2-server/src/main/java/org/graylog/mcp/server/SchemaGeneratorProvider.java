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
package org.graylog.mcp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.jsonschema.GraylogSchemaGeneratorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Provider for the JSON Schema generator used by MCP tools.
 * Consolidates the base schema generation configuration and allows plugins
 * to contribute additional modules via the {@link McpSchemaModule} binding.
 */
@Singleton
public class SchemaGeneratorProvider {
    private static final Logger LOG = LoggerFactory.getLogger(SchemaGeneratorProvider.class);

    private final SchemaGenerator generator;

    @Inject
    public SchemaGeneratorProvider(ObjectMapper objectMapper, @McpSchemaModule Set<Module> contributedModules) {
        LOG.debug("Initializing SchemaGenerator with {} contributed modules", contributedModules.size());
        if (!contributedModules.isEmpty()) {
            LOG.debug("Contributed modules: {}", contributedModules.stream()
                    .map(m -> m.getClass().getSimpleName())
                    .toList());
        }

        var builder = GraylogSchemaGeneratorConfig.baseBuilder(objectMapper);

        // Add all contributed modules from plugins
        for (Module module : contributedModules) {
            LOG.debug("Adding schema module: {}", module.getClass().getSimpleName());
            builder = builder.with(module);
        }

        SchemaGeneratorConfig config = builder.build();
        this.generator = new SchemaGenerator(config);
        LOG.debug("SchemaGenerator initialized");
    }

    /**
     * Returns the configured schema generator instance.
     * This generator is shared across all MCP tools.
     */
    public SchemaGenerator get() {
        return generator;
    }
}
