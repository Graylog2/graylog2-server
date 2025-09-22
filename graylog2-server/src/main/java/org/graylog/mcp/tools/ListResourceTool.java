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

import au.com.bytecode.opencsv.CSVWriter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import org.graylog.grn.GRNType;
import org.graylog.grn.GRNTypes;
import org.graylog.mcp.server.PaginatedList;
import org.graylog.mcp.server.ResourceProvider;
import org.graylog.mcp.server.Tool;

import java.io.StringWriter;
import java.util.Map;

public class ListResourceTool extends Tool<ListResourceTool.Parameters, String> {
    public static String NAME = "list_resource";

    private final Map<GRNType, ? extends ResourceProvider> resourceProviders;

    @Inject
    public ListResourceTool(ObjectMapper objectMapper, Map<GRNType, ? extends ResourceProvider> resourceProviders) {
        super(objectMapper,
                new TypeReference<>() {},
                NAME,
                "List all Resources available in Graylog for a given resource type",
                "A csv-formatted string listing each resource GRN and Name");
        this.resourceProviders = resourceProviders;
    }

    @Override
    public String apply(PermissionHelper permissionHelper, ListResourceTool.Parameters parameters) {
        final StringWriter writer = new StringWriter();
        final CSVWriter csvWriter = new CSVWriter(writer);
        GRNType grnType = switch (parameters.type.toLowerCase().replace(' ', '_')) {
            case "streams", "stream" -> GRNTypes.STREAM;
            case "dashboards", "dashboard" -> GRNTypes.DASHBOARD;
            case "event_definitions", "event_definition", "eventdefinitions", "eventdefinition"  -> GRNTypes.EVENT_DEFINITION;
            default -> throw new IllegalArgumentException("Unsupported type " + parameters.type);
        };
        csvWriter.writeNext(new String[]{"grn", "name"});
        try (java.util.stream.Stream<McpSchema.Resource> resources = this.resourceProviders.get(grnType)
                .list(new PaginatedList.Cursor(null)).stream()) {
//                .list(new PaginatedList.Cursor(parameters.cursor)).stream()) {
            resources.forEach(resource -> csvWriter.writeNext(new String[]{
                    resource.uri(),
                    resource.name()
            }));
        }
        return writer.toString();
    }

    public static class Parameters {
        private String type;
//        String cursor;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}
