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
import com.fasterxml.jackson.annotation.JsonProperty;
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
import java.util.List;
import java.util.Locale;
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
        final int pageSize = Math.min(Math.max(parameters.getPerPage(), 1), 20);

        GRNType grnType = switch (parameters.type.toLowerCase(Locale.US).replace(' ', '_')) {
            case "streams", "stream" -> GRNTypes.STREAM;
            case "dashboards", "dashboard" -> GRNTypes.DASHBOARD;
            case "event_definitions", "event_definition", "eventdefinitions", "eventdefinition"  -> GRNTypes.EVENT_DEFINITION;
            default -> throw new IllegalArgumentException("Unsupported type " + parameters.type);
        };

        PaginatedList.Cursor cursor = new PaginatedList.Cursor(PaginatedList.decodeCursor(parameters.cursor));
        List<McpSchema.Resource> resources = this.resourceProviders.get(grnType).list(cursor, pageSize);
        boolean hasMore = resources.size() == pageSize + 1;
        List<McpSchema.Resource> pageItems = hasMore ? resources.subList(0, pageSize) : resources;
        String next = null;
        if (hasMore && !pageItems.isEmpty()) {
            String lastGrn = pageItems.getLast().uri();
            next = PaginatedList.encodeCursor(lastGrn);
        }

        try {
            PaginatedList<List<String>> payload = new PaginatedList<>(
                    pageItems.stream().map(resource -> List.of(resource.uri(), resource.name())).toList(),
                    next == null ? null : new PaginatedList.Cursor(PaginatedList.encodeCursor(next))
            );
            return new ObjectMapper().writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize response", e);
        }

    }

    public static class Parameters {
        @JsonProperty(value = "graylog_resource_type", required = true)
        private String type;

        @JsonProperty(value = "per_page", required = false)
        private int perPage;

        @JsonProperty(value = "page_cursor", required = false)
        private String cursor;

        public String getType() { return type; }
        public String getCursor() { return cursor; }
        public int getPerPage() { return perPage; }

        public void setType(String type) { this.type = type; }
        public void setCursor(String cursor) { this.cursor = cursor; }
        public void setPerPage(String perPage) { this.perPage = Integer.parseInt(perPage); }
    }
}
