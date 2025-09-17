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
import jakarta.inject.Inject;
import org.graylog.mcp.server.Tool;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.StreamService;

import java.io.StringWriter;

// TODO convert this into a resource, tools should not be simple "read operations" but perform actions
public class ListStreamsTool extends Tool<ListStreamsTool.Parameters, String> {
    public static String NAME = "list_streams";

    private final StreamService streamService;

    @Inject
    public ListStreamsTool(ObjectMapper objectMapper, StreamService streamService) {
        super(objectMapper,
                new TypeReference<>() {},
                NAME,
                "List all streams available in Graylog",
                """
                        The streams list.
                        """);
        this.streamService = streamService;
    }

    @Override
    public String apply(PermissionHelper permissionHelper, ListStreamsTool.Parameters unused) {
        final StringWriter writer = new StringWriter();
        final CSVWriter csvWriter = new CSVWriter(writer);

        csvWriter.writeNext(new String[]{"id", "name", "description", "index_set"});
        try (java.util.stream.Stream<Stream> dtos = streamService.streamAllDTOs()) {
            dtos.filter(stream -> permissionHelper.isPermitted(RestPermissions.STREAMS_READ, stream.getId()))
                    .forEach(stream -> csvWriter.writeNext(new String[]{
                    stream.getId(),
                    stream.getTitle(),
                    stream.getDescription(),
                    stream.getIndexSet() == null ? "Unknown indexset" : stream.getIndexSet().getConfig().title()}));
        }
        return writer.toString();
    }

    public static class Parameters {}
}
