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
import jakarta.inject.Inject;
import org.glassfish.jersey.uri.UriTemplate;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNType;
import org.graylog.grn.GRNTypes;
import org.graylog.mcp.server.PaginatedList;
import org.graylog.mcp.server.ResourceProvider;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamService;
import jakarta.annotation.Nullable;

import java.net.URI;
import java.util.List;

public class StreamResourceProvider extends ResourceProvider {
    public static final GRNType GRN_TYPE = GRNTypes.STREAM;
    private static final String GRN_TEMPLATE = GRN_TYPE.toGRN("{stream_id}").toString();

    private final StreamService streamService;
    private final GRNRegistry grnRegistry;

    @Inject
    public StreamResourceProvider(StreamService streamService, GRNRegistry grnRegistry) {
        this.streamService = streamService;
        this.grnRegistry = grnRegistry;
    }

    @Override
    public Template resourceTemplate() {
        return new Template(
                new UriTemplate(GRN_TEMPLATE),
                "Streams",
                "Streams",
                "Access streams in this Graylog cluster",
                "application/json"
        );
    }

    @Override
    public McpSchema.Resource read(URI uri) throws NotFoundException {
        final GRN grn = grnRegistry.parse(uri.toString());
        if (!grn.isType(GRNTypes.STREAM)) {
            throw new IllegalArgumentException("Invalid GRN URI, expected a stream GRN: " + uri);
        }
        final Stream stream = streamService.load(grn.entity());
        return McpSchema.Resource.builder()
                .name(stream.getTitle())
                .description(stream.getDescription())
//                .uri(URI.create(GRN.builder().grnType(GRNTypes.STREAM).entity(stream.getId()).build().toString()).toString())  // <--- This throws a "Missing required properties: type" error
                .uri(grn.toString())
                .build();
    }

    @Override
    public List<McpSchema.Resource> list(@Nullable PaginatedList.Cursor cursor, @Nullable Integer pageSize) {
        // TODO adapting pagination is a bit awkward right now, we'll simply skip it to make it work
        try (var dtos = streamService.streamAllDTOs()) {
            return dtos
                    .map(stream -> new McpSchema.Resource(
                            GRN_TYPE.toGRN(stream.getId()).toString(),
                            stream.getTitle(),
                            stream.getTitle(),
                            stream.getDescription(),
                            null,
                            null,
                            null,
                            null))
                    .toList();
        }
    }

}
