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
package org.graylog.mcp.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import org.graylog.grn.GRNType;
import org.graylog.grn.GRNTypes;
import org.graylog.mcp.server.ResourceProvider;
import org.graylog.mcp.server.SchemaGeneratorProvider;
import org.graylog.mcp.server.Tool;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ListResourceTool extends Tool<ListResourceTool.Parameters, ListResourceTool.Result> {
    public static String NAME = "list_resource";

    private final Map<GRNType, ? extends ResourceProvider> resourceProviders;

    @Inject
    public ListResourceTool(ObjectMapper objectMapper,
                            SchemaGeneratorProvider schemaGeneratorProvider, Map<GRNType, ? extends ResourceProvider> resourceProviders) {
        super(objectMapper,
                schemaGeneratorProvider,
                new TypeReference<>() {},
                new TypeReference<>() {},
                NAME,
                "List Resources",
                """
                        List all Resources available in the server for one resource type out of the following: {stream,dashboard,event_definition}.
                        Returns: A list of tuples with both the GRN and name of each resource.
                        """);
        this.resourceProviders = resourceProviders;
    }

    @Override
    public ListResourceTool.Result apply(PermissionHelper permissionHelper, ListResourceTool.Parameters parameters) {
        GRNType grnType = switch (parameters.type.toLowerCase(Locale.US).replace(' ', '_')) {
            case "streams", "stream" -> GRNTypes.STREAM;
            case "dashboards", "dashboard" -> GRNTypes.DASHBOARD;
            case "event_definitions", "event_definition", "eventdefinitions", "eventdefinition" ->
                    GRNTypes.EVENT_DEFINITION;
            default -> throw new IllegalArgumentException("Unsupported type " + parameters.type);
        };

        List<McpSchema.Resource> resources = this.resourceProviders.get(grnType).list(permissionHelper, null, null);
        return new Result(resources);
    }

    public record Result(@JsonProperty("resources") List<McpSchema.Resource> resources) {}

    public static class Parameters {
        @JsonProperty(value = "resource_type", required = true)
        private String type;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}
