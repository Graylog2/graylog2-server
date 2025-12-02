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

import com.fasterxml.jackson.annotation.JsonAlias;
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

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.lang.annotation.ElementType.TYPE;


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

    /**
     * Hints about tool behavior.
     * <ul>
     *     <li><code>readOnly</code>: the tool <b>does not modify</b> its environment.</li>
     *     <li><code>destructive</code>: the tool may <b>perform destructive updates</b>.</li>
     *     <li><code>idempotent</code>: repeated calls with same args have <b>no additional effect</b>.</li>
     *     <li><code>openWorld</code>: the tool interacts with <b>external entities</b>.</li>
     * </ul>
     */
    @Retention(RUNTIME) @Target(TYPE)
    public @interface Behavior {
        boolean readOnly() default false;
        boolean destructive() default false;
        boolean idempotent() default false;
        boolean openWorld() default false;
    }

    public enum ToolBehavior {
        READ_ONLY(1 << 0),
        DESTRUCTIVE(1 << 1),
        IDEMPOTENT(1 << 2),
        OPEN_WORLD(1 << 3);

        private final int mask;
        ToolBehavior(int mask) { this.mask = mask; }
        public int mask() { return mask; }
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
    private final EnumSet<ToolBehavior> behaviors;
    private boolean enabled;
    private OutputFormat outputFormatOverride;

    protected Tool(
            ToolContext context,
            TypeReference<P> parameterType,
            TypeReference<O> outputType,
            String name,
            String title,
            String description
    ) {
        this.parameterType = parameterType;
        this.name = name;
        this.title = title;
        this.description = description;

        this.behaviors = EnumSet.noneOf(ToolBehavior.class);
        Behavior behavior = this.getClass().getAnnotation(Behavior.class);
        if (behavior != null) {
            if (behavior.readOnly()) {
                this.behaviors.add(ToolBehavior.READ_ONLY);
            }
            if (behavior.destructive()) {
                this.behaviors.add(ToolBehavior.DESTRUCTIVE);
            }
            if (behavior.idempotent()) {
                this.behaviors.add(ToolBehavior.IDEMPOTENT);
            }
            if (behavior.openWorld()) {
                this.behaviors.add(ToolBehavior.OPEN_WORLD);
            }
        }

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

        this.outputFormatOverride = OutputFormat.NO_OVERRIDE;
        this.enabled = true;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    protected boolean isOutputSchemaEnabled() {
        return clusterConfigService.getOrDefault(McpConfiguration.class, McpConfiguration.DEFAULT_VALUES)
                .enableOutputSchema();
    }

    public boolean isOutputFormatOverridden() {
        return !outputFormatOverride.equals(OutputFormat.NO_OVERRIDE);
    }

    public void setOutputFormatOverride(OutputFormat outputFormatOverride) {
        this.outputFormatOverride = outputFormatOverride;
    }

    protected boolean useStringOutput() {
        return clusterConfigService.getOrDefault(McpConfiguration.class, McpConfiguration.DEFAULT_VALUES)
                .useStringOutput();
    }

    public OutputFormat getOutputFormat() {
        if (isOutputFormatOverridden()) {
            return outputFormatOverride;
        }
        return useStringOutput() ? OutputFormat.MARKDOWN : OutputFormat.JSON;
    }

    protected ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public String getProductName() { return productName; }

    public String getCategory() {
        final String packageName = this.getClass().getPackageName();

        if (packageName.equals("org.graylog.mcp.tools")) {
            return "core";
        }

        if (packageName.startsWith("org.graylog.plugins.mcp.tools.")) {
            return packageName.substring("org.graylog.plugins.mcp.tools.".length());
        }

        return "external";
    }

    public byte bitwiseBehavior() {
        int bits = 0;
        for (ToolBehavior b : behaviors) {
            bits |= b.mask();
        }
        return (byte) bits;
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
    public Optional<McpSchema.JsonSchema> inputSchema() {
        return Optional.ofNullable(inputSchema);
    }

    @JsonProperty
    public Optional<Map<String, Object>> outputSchema() {
        return isOutputSchemaEnabled() ? Optional.ofNullable(outputSchema) : Optional.empty();
    }

    /**
     * Calls the tool implementation with the raw parameter map.
     * Internally this will get converted into the actual parameter type for type safety.
     *
     * @param parameterMap raw parameter map
     * @return the return value of the tool call
     */
    public ToolResult<O> apply(PermissionHelper permissionHelper, Map<String, Object> parameterMap) {
        if (!isEnabled()) {
            throw new IllegalStateException("Tool is disabled");
        }

        final P p = objectMapper.convertValue(parameterMap, parameterType);
        if (useStringOutput()) {
            return new ToolResult.Text<>(applyAsText(permissionHelper, p));
        }
        return new ToolResult.Data<>(apply(permissionHelper, p));
    }

    protected abstract O apply(PermissionHelper permissionHelper, P parameters);

    protected String applyAsText(PermissionHelper permissionHelper, P parameters) {
        return apply(permissionHelper, parameters).toString();
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

    public enum OutputFormat {
        @JsonProperty("json")
        @JsonAlias({"JSON", "Json", "Structured", "structured"})
        JSON,
        @JsonProperty("markdown")
        @JsonAlias({"MARKDOWN", "Markdown", "String", "string"})
        MARKDOWN,
        @JsonProperty("default")
        @JsonAlias({"DEFAULT", "Default", "NONE", "None", "none", "NULL", "Null", "null", ""})
        NO_OVERRIDE
    }
}
