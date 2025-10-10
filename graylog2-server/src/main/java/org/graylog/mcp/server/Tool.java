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
import com.github.victools.jsonschema.generator.SchemaGenerator;
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

    private final ObjectMapper objectMapper;
    private final TypeReference<P> parameterType;
    private final String name;
    private final String title;
    private final String description;
    private final String inputSchema;
    private final String outputSchema;

    protected Tool(
            ObjectMapper objectMapper,
            SchemaGeneratorProvider schemaGeneratorProvider,
            TypeReference<P> parameterType,
            TypeReference<O> outputType,
            String name,
            String title,
            String description) {
        this.objectMapper = objectMapper;
        this.parameterType = parameterType;
        this.name = name;
        this.title = title;
        this.description = description;

        // Get the schema generator with all contributed modules
        SchemaGenerator generator = schemaGeneratorProvider.get();

        // we can precompute the schema for our parameters, it's statically known
        this.inputSchema = generator.generateSchema(parameterType.getType()).toString();
        // if our tool produces anything other than a String, we want to create a JSON schema for it
        if (String.class.equals(outputType.getType())) {
            this.outputSchema = null;
        } else {
            this.outputSchema = generator.generateSchema(outputType.getType()).toString();
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
