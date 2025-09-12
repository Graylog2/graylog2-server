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
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;

import java.util.Map;

/**
 * The base class for MCP tools.
 *
 * @param <P> Parameter type
 * @param <O> Output type
 */
public abstract class Tool<P, O> {

    // MCP inexplicable uses Draft 7 of JSON Schema
    private static final SchemaGeneratorConfigBuilder CONFIG_BUILDER = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_7, OptionPreset.PLAIN_JSON);
    private static final SchemaGeneratorConfig CONFIG = CONFIG_BUILDER.with(new EmptyObjectAsObjectModule()).build();
    private static final SchemaGenerator GENERATOR = new SchemaGenerator(CONFIG);

    private final ObjectMapper objectMapper;
    private final TypeReference<P> parameterType;
    private final String name;
    private final String title;
    private final String description;
    private final String inputSchema;

    protected Tool(ObjectMapper objectMapper, TypeReference<P> parameterType, String name, String title, String description) {
        this.objectMapper = objectMapper;
        this.parameterType = parameterType;
        this.name = name;
        this.title = title;
        this.description = description;

        // we can precompute the schema for our parameters, it's statically known
        this.inputSchema = GENERATOR.generateSchema(new TypeReference<P>() {}.getType()).toString();
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

    /**
     * Calls the tool implementation with the raw parameter map.
     * Internally this will get converted into the actual parameter type for type safety.
     *
     * @param parameterMap raw parameter map
     * @return the return value of the tool call
     */
    public O apply(Map<String, Object> parameterMap) {
        final P p = objectMapper.convertValue(parameterMap, parameterType);
        return apply(p);
    }

    protected abstract O apply(P parameters);
}
