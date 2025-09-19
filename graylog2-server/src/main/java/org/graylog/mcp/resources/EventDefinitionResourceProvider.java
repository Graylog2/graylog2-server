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
package org.graylog.mcp.resources;

import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.glassfish.jersey.uri.UriTemplate;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNType;
import org.graylog.grn.GRNTypes;
import org.graylog.mcp.server.PaginatedList;
import org.graylog.mcp.server.ResourceProvider;
import org.graylog2.database.NotFoundException;

import java.net.URI;
import java.util.List;

public class EventDefinitionResourceProvider extends ResourceProvider {
    public static final GRNType GRN_TYPE = GRNTypes.EVENT_DEFINITION;
    private static final String GRN_TEMPLATE = GRN_TYPE.toGRN("{event_definition_id}").toString();

    private final DBEventDefinitionService eventDefinitionService;
    private final GRNRegistry grnRegistry;

    @Inject
    public EventDefinitionResourceProvider(DBEventDefinitionService eventDefinitionService, GRNRegistry grnRegistry) {
        this.eventDefinitionService = eventDefinitionService;
        this.grnRegistry = grnRegistry;
    }

    @Override
    public Template resourceTemplate() {
        return new Template(
                new UriTemplate(GRN_TEMPLATE),
                "EventDefinition",
                "EventDefinition",
                "Access Event Definitions in this Graylog cluster",
                "application/json"
        );
    }

    @Override
    public McpSchema.Resource read(URI uri) throws NotFoundException {
        final GRN grn = grnRegistry.parse(uri.toString());
        if (!grn.isType(GRNTypes.EVENT_DEFINITION)) {
            throw new IllegalArgumentException("Invalid GRN URI, expected an Event Definition GRN: " + uri);
        }
        final EventDefinitionDto eventDefinition = eventDefinitionService.get(grn.entity()).orElseThrow(NotFoundException::new);
        return McpSchema.Resource.builder()
                .name(eventDefinition.title())
                .description(eventDefinition.description())
                .uri(grn.toString())
                .build();
    }

    @Override
    public List<McpSchema.Resource> list(@Nullable PaginatedList.Cursor cursor) {
        try (var dtos = eventDefinitionService.streamAll()) {
            return dtos
                    .map(eventDefinition -> new McpSchema.Resource(
                            GRN_TYPE.toGRN(eventDefinition.id()).toString(),
                            eventDefinition.title(),
                            eventDefinition.title(),
                            eventDefinition.description(),
                            null,
                            null,
                            null,
                            null))
                    .toList();
        }
    }

}
