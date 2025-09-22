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
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNType;
import org.graylog.grn.GRNTypes;
import org.graylog.mcp.server.PaginatedList;
import org.graylog.mcp.server.ResourceProvider;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog2.database.NotFoundException;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.search.SearchQuery;

import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

public class DashboardResourceProvider extends ResourceProvider {
    public static final GRNType GRN_TYPE = GRNTypes.DASHBOARD;
    private static final String GRN_TEMPLATE = GRN_TYPE.toGRN("{dashboard_id}").toString();

    private final ViewService viewService;
    private final GRNRegistry grnRegistry;

    @Inject
    public DashboardResourceProvider(ViewService viewService, GRNRegistry grnRegistry) {
        this.viewService = viewService;
        this.grnRegistry = grnRegistry;
    }

    @Override
    public Template resourceTemplate() {
        return new Template(
                new UriTemplate(GRN_TEMPLATE),
                "Dashboards",
                "Dashboards",
                "Access dashboards in this Graylog cluster",
                "application/json"
        );
    }

    @Override
    public McpSchema.Resource read(URI uri) throws NotFoundException {
        final GRN grn = grnRegistry.parse(uri.toString());
        if (!grn.isType(GRNTypes.DASHBOARD)) {
            throw new IllegalArgumentException("Invalid GRN URI, expected a Dashboard GRN: " + uri);
        }
        final ViewDTO dashboard = viewService.get(grn.entity()).orElseThrow(NotFoundException::new);
        return McpSchema.Resource.builder()
                .name(dashboard.title())
                .description(dashboard.description())
                .uri(grn.toString())
                .build();
    }

    @Override
    public List<McpSchema.Resource> list(@Nullable PaginatedList.Cursor cursor, @Nullable Integer pageSize) {
        final Stream<ViewDTO> resultStream = viewService.searchPaginatedByType(
                ViewDTO.Type.DASHBOARD,
                new SearchQuery(""),
                dashboard -> true,
                SortOrder.ASCENDING, ViewDTO.FIELD_ID, 1, 0).stream();

        try (resultStream) {
            return resultStream
                    .map(dashboard -> new McpSchema.Resource(
                            GRN_TYPE.toGRN(dashboard.id()).toString(),
                            dashboard.title(),
                            dashboard.title(),
                            dashboard.description(),
                            null,
                            null,
                            null,
                            null))
                    .toList();
        }
    }

}
