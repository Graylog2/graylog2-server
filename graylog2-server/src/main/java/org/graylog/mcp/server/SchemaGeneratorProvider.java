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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.DefaultValue;
import org.graylog.jsonschema.EmptyObjectAsObjectModule;
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
    public SchemaGeneratorProvider(@McpSchemaModule Set<Module> contributedModules) {
        LOG.debug("Initializing SchemaGenerator with {} contributed modules", contributedModules.size());
        if (!contributedModules.isEmpty()) {
            LOG.debug("Contributed modules: {}", contributedModules.stream()
                    .map(m -> m.getClass().getSimpleName())
                    .toList());
        }

        var builder = createBaseBuilder();

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

    /**
     * Creates the base schema generator configuration with standard modules
     * and field/method resolution rules.
     */
    private SchemaGeneratorConfigBuilder createBaseBuilder() {
        var builder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                .with(Option.FIELDS_DERIVED_FROM_ARGUMENTFREE_METHODS, Option.NONSTATIC_NONVOID_NONGETTER_METHODS)
                .with(new EmptyObjectAsObjectModule())
                .with(new JacksonModule(
                        JacksonOption.INCLUDE_ONLY_JSONPROPERTY_ANNOTATED_METHODS,
                        JacksonOption.FLATTENED_ENUMS_FROM_JSONVALUE,
                        JacksonOption.RESPECT_JSONPROPERTY_REQUIRED,
                        JacksonOption.RESPECT_JSONPROPERTY_ORDER))
                .with(new JakartaValidationModule());

        // Configure field default value resolution
        builder.forFields()
                .withDefaultResolver(field -> {
                    // Jakarta @DefaultValue takes precedence
                    final DefaultValue jakarta = field.getAnnotationConsideringFieldAndGetter(DefaultValue.class);
                    if (jakarta != null && jakarta.value() != null) {
                        return jakarta.value();
                    }
                    // Fall back to Jackson @JsonProperty defaultValue
                    final JsonProperty jackson = field.getAnnotationConsideringFieldAndGetter(JsonProperty.class);
                    return jackson == null || jackson.defaultValue().isEmpty() ? null : jackson.defaultValue();
                });

        // Configure method handling for bean getters, @JsonProperty methods, and AutoValue accessors
        builder.forMethods()
                .withIgnoreCheck(m -> {
                    // Keep bean getters
                    if (m.isGetter()) return false;

                    // Keep explicit @JsonProperty methods
                    if (m.getAnnotation(JsonProperty.class) != null) return false;

                    // Keep AutoValue-style accessors: zero-arg, non-void
                    boolean zeroArgNonVoid = m.getRawMember().getParameterCount() == 0 &&
                            m.getRawMember().getReturnType() != Void.TYPE;
                    if (!zeroArgNonVoid) return true;

                    // If there's a field with the same name, prefer the field (avoid dupes)
                    String candidate = m.getName();
                    return m.getDeclaringType()
                            .getMemberFields()
                            .stream()
                            .anyMatch(f -> f.getName().equals(candidate));
                })
                .withPropertyNameOverrideResolver(m -> {
                    // @JsonProperty wins
                    JsonProperty jp = m.getAnnotation(JsonProperty.class);
                    if (jp != null && jp.value() != null && !jp.value().isEmpty()) {
                        return jp.value();
                    }

                    // Bean getters resolve to their normal JSON names (getFoo -> "foo")
                    if (m.isGetter()) {
                        return m.getSchemaPropertyName();
                    }

                    // AutoValue-style accessors: use the method name without "()"
                    return m.getName();
                });

        return builder;
    }
}
