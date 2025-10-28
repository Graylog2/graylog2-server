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
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import org.graylog.mcp.config.McpConfiguration;
import org.graylog.mcp.tools.PermissionHelper;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.web.customization.CustomizationConfig;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

/**
 * The base class for MCP tools.
 *
 * @param <P> Parameter type
 * @param <O> Output type
 */
public abstract class Tool<P, O> {
    public record ToolContext(
            ObjectMapper objectMapper,
            SchemaGeneratorProvider schemaGeneratorProvider,
            CustomizationConfig  customizationConfig,
            ClusterConfigService clusterConfigService
    ) {
        @Inject
        public ToolContext {}
    }

    private final ObjectMapper objectMapper;
    private final ClusterConfigService clusterConfigService;
    private final String productName;

    private final TypeReference<P> parameterType;
    private final String name;
    private final String title;
    private final String description;
    private final McpSchema.JsonSchema inputSchema;
    private final Map<String, Object> outputSchema;

    protected Tool(
            ToolContext context,
            TypeReference<P> parameterType,
            TypeReference<O> outputType,
            String name,
            String title,
            String description) {
        this.parameterType = parameterType;
        this.name = name;
        this.title = title;
        this.description = description;

        this.objectMapper = context.objectMapper();
        this.clusterConfigService = context.clusterConfigService();
        this.productName = context.customizationConfig().productName();

        // Get the schema generator with all contributed modules
        SchemaGenerator generator = context.schemaGeneratorProvider().get();

        // we can precompute the schema for our parameters, it's statically known
        final var inputSchemaNode = generator.generateSchema(parameterType.getType());
        if (inputSchemaNode.isEmpty()) {
            this.inputSchema = null;
        } else {
            this.inputSchema = objectMapper.convertValue(inputSchemaNode, McpSchema.JsonSchema.class);
        }
        // if our tool produces anything other than a String, we want to create a JSON schema for it
        if (String.class.equals(outputType.getType())) {
            this.outputSchema = null;
        } else {
            this.outputSchema = objectMapper.convertValue(generator.generateSchema(outputType.getType()), new TypeReference<Map<String, Object>>() {});
        }
    }

    protected boolean isStructuredOutputSet() {
        return clusterConfigService.getOrDefault(McpConfiguration.class, McpConfiguration.DEFAULT_VALUES
        ).useStructuredOutput();
    }

    protected ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public String getProductName() { return productName; }

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
    public Optional<McpSchema.JsonSchema> inputSchema() {
        return Optional.ofNullable(inputSchema);
    }

    @JsonProperty
    public Optional<Map<String, Object>> outputSchema() {
        return isStructuredOutputSet() ? Optional.ofNullable(outputSchema) : Optional.empty();
    }

    /**
     * Calls the tool implementation with the raw parameter map.
     * Internally this will get converted into the actual parameter type for type safety.
     *
     * @param parameterMap raw parameter map
     * @return the return value of the tool call
     */
    public ToolResult<O> apply(PermissionHelper permissionHelper, Map<String, Object> parameterMap) {
        final P p = objectMapper.convertValue(parameterMap, parameterType);
        if (!isStructuredOutputSet()) {
            return new ToolResult.Text<>(applyAsText(permissionHelper, p));
        }
        return new ToolResult.Data<>(apply(permissionHelper, p));
    }

    protected abstract O apply(PermissionHelper permissionHelper, P parameters);

    protected String applyAsText(PermissionHelper permissionHelper, P parameters) {
        throw new UnsupportedOperationException("Text output not implemented");
    }

    public sealed interface ToolResult<T> {
        record Text<T>(@JsonValue String value) implements ToolResult<T> {
            @Override
            public @NotNull String toString() { return value != null ? value : "(empty)"; }
        }
        record Data<T>(@JsonValue T value) implements ToolResult<T> {
            @Override
            public @NotNull String toString() {
                return value != null && !value.toString().isBlank() ? value.toString() : "{}";
            }
        }
    }
}
