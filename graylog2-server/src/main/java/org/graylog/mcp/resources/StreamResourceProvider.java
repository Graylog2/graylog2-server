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

import com.google.common.base.Strings;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import org.glassfish.jersey.uri.UriTemplate;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNType;
import org.graylog.grn.GRNTypes;
import org.graylog.mcp.server.ResourceProvider;
import org.graylog.mcp.tools.PermissionHelper;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.StreamService;
import org.graylog2.web.customization.CustomizationConfig;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.graylog2.shared.utilities.StringUtils.f;

public class StreamResourceProvider extends ResourceProvider {
    public static final GRNType GRN_TYPE = GRNTypes.STREAM;
    private static final String GRN_TEMPLATE = GRN_TYPE.toGRN("{stream_id}").toString();

    private final StreamService streamService;
    private final GRNRegistry grnRegistry;
    private final String productName;

    @Inject
    public StreamResourceProvider(StreamService streamService,
                                  CustomizationConfig customizationConfig,
                                  GRNRegistry grnRegistry) {
        this.streamService = streamService;
        this.grnRegistry = grnRegistry;
        this.productName = customizationConfig.productName();
    }

    @Override
    public Template resourceTemplate() {
        return new Template(
                new UriTemplate(GRN_TEMPLATE),
                "Streams",
                "Streams",
                f("Access streams in this %s cluster", productName),
                MediaType.APPLICATION_JSON
        );
    }

    @Override
    public Optional<McpSchema.Resource> read(final PermissionHelper permissionHelper, URI uri) {
        final GRN grn = grnRegistry.parse(uri.toString());
        if (!grn.isType(GRNTypes.STREAM)) {
            throw new IllegalArgumentException("Invalid GRN URI, expected a stream GRN: " + uri);
        }
        if (!permissionHelper.isPermitted(RestPermissions.STREAMS_READ, grn.entity())) {
            return Optional.empty();
        }
        try {
            final Stream stream = streamService.load(grn.entity());
            // MCP SDK 2.0.0 rejects a null/empty Resource name; fall back to the id.
            final String name = Strings.isNullOrEmpty(stream.getTitle()) ? stream.getId() : stream.getTitle();
            return Optional.of(McpSchema.Resource.builder(grn.toString(), name)
                    .description(stream.getDescription())
                    .build());
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<McpSchema.Resource> list(final PermissionHelper permissionHelper) {
        // TODO adapting pagination is a bit awkward right now, we'll simply skip it to make it work
        try (var dtos = streamService.streamAllDTOs()) {
            return dtos
                    .filter(stream -> permissionHelper.isPermitted(RestPermissions.STREAMS_READ, stream.getId()))
                    .map(stream -> McpSchema.Resource.builder(
                                    GRN_TYPE.toGRN(stream.getId()).toString(),
                                    // MCP SDK 2.0.0 rejects a null/empty Resource name; fall back to the id.
                                    Strings.isNullOrEmpty(stream.getTitle()) ? stream.getId() : stream.getTitle())
                            .title(stream.getTitle())
                            .description(stream.getDescription())
                            .build())
                    .toList();
        }
    }

}
