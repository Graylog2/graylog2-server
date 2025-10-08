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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
import jakarta.ws.rs.DefaultValue;
import org.graylog.jsonschema.EmptyObjectAsObjectModule;
import org.graylog.mcp.tools.PermissionHelper;

import java.util.Map;
import java.util.Optional;

/**
 * The base class for MCP tools.
 *
 * @param <P> Parameter type
 * @param <O> Output type
 */
public abstract class Tool<P, O> {

    private static final SchemaGeneratorConfig CONFIG;

    static {
        var builder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON).with(
                        Option.FIELDS_DERIVED_FROM_ARGUMENTFREE_METHODS, Option.NONSTATIC_NONVOID_NONGETTER_METHODS)
                .with(new EmptyObjectAsObjectModule())
                .with(new JacksonModule(JacksonOption.INCLUDE_ONLY_JSONPROPERTY_ANNOTATED_METHODS,
                                        JacksonOption.RESPECT_JSONPROPERTY_REQUIRED,
                                        JacksonOption.RESPECT_JSONPROPERTY_ORDER))
                .with(new JakartaValidationModule());
        builder.forFields()
                // peel out default values from annotations, supports both Jakarta & Jackson annotations
                // but gives Jakarta precedence
                .withDefaultResolver(field -> {
                    final DefaultValue jakarta = field.getAnnotationConsideringFieldAndGetter(DefaultValue.class);
                    if (jakarta != null && jakarta.value() != null) {
                        return jakarta.value();
                    }
                    final JsonProperty jackson = field.getAnnotationConsideringFieldAndGetter(JsonProperty.class);
                    return jackson == null || jackson.defaultValue().isEmpty() ? null : jackson.defaultValue();
                });
        // Methods: allow (1) bean getters, (2) @JsonProperty methods, (3) zero-arg non-void accessors (AutoValue).
        builder.forMethods().withIgnoreCheck(m -> {
                    // keep bean getters
                    if (m.isGetter()) return false;

                    // keep explicit @JsonProperty methods
                    if (m.getAnnotation(JsonProperty.class) != null) return false;

                    // keep AutoValue-style accessors: zero-arg, non-void
                    boolean zeroArgNonVoid = m.getRawMember().getParameterCount() == 0 &&
                                             m.getRawMember().getReturnType() != Void.TYPE;
                    if (!zeroArgNonVoid) return true;

                    // if there's a field with the same name, prefer the field (avoid dupes like "type" + "type()")
                    String candidate = m.getName(); // e.g., "fields" from "fields()"
                    return m.getDeclaringType()
                            .getMemberFields()
                            .stream()
                            .anyMatch(f -> f.getName().equals(candidate)); // ignore method when a same-named field exists
                })
                .withPropertyNameOverrideResolver(m -> {
                    // @JsonProperty wins (also covers non-bean methods if you annotate them)
                    JsonProperty jp = m.getAnnotation(JsonProperty.class);
                    if (jp != null && jp.value() != null && !jp.value().isEmpty()) {
                        return jp.value();
                    }

                    // Bean getters resolve to their normal JSON names (getFoo/isBar -> "foo"/"bar")
                    if (m.isGetter()) {
                        return m.getSchemaPropertyName();
                    }

                    // AutoValue-style accessors: use the method name without "()"
                    // e.g., "fields()" -> "fields"
                    return m.getName();
                });
        CONFIG = builder.build();
    }

    private static final SchemaGenerator GENERATOR = new SchemaGenerator(CONFIG);

    private final ObjectMapper objectMapper;
    private final TypeReference<P> parameterType;
    private final String name;
    private final String title;
    private final String description;
    private final String inputSchema;
    private final String outputSchema;

    protected Tool(ObjectMapper objectMapper, TypeReference<P> parameterType, TypeReference<O> outputType, String name, String title, String description) {
        this.objectMapper = objectMapper;
        this.objectMapper.registerModule(new JodaModule());
        this.parameterType = parameterType;
        this.name = name;
        this.title = title;
        this.description = description;

        // we can precompute the schema for our parameters, it's statically known
        this.inputSchema = GENERATOR.generateSchema(parameterType.getType()).toString();
        // if our tool produces anything other than a String, we want to create a JSON schema for it
        if (String.class.equals(outputType.getType())) {
            this.outputSchema = null;
        } else {
            this.outputSchema = GENERATOR.generateSchema(outputType.getType()).toString();
        }
    }

    protected ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @JsonProperty
    public String name() {
        return name;
    }

    @JsonProperty
    public String title() {
        return title;
    }

    @JsonProperty
    public String description() {
        return description;
    }

    @JsonProperty
    public String inputSchema() {
        return inputSchema;
    }

    @JsonProperty
    public Optional<String> outputSchema() {
        return Optional.ofNullable(outputSchema);
    }

    /**
     * Calls the tool implementation with the raw parameter map.
     * Internally this will get converted into the actual parameter type for type safety.
     *
     * @param parameterMap raw parameter map
     * @return the return value of the tool call
     */
    public O apply(PermissionHelper permissionHelper, Map<String, Object> parameterMap) {
        final P p = objectMapper.convertValue(parameterMap, parameterType);
        return apply(permissionHelper, p);
    }

    protected abstract O apply(PermissionHelper permissionHelper, P parameters);
}
