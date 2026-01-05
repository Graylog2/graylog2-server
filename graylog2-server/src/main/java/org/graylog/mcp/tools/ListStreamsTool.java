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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import org.graylog.mcp.server.SchemaGeneratorProvider;
import org.graylog.mcp.server.Tool;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.StreamService;
import org.graylog2.web.customization.CustomizationConfig;

import java.util.Map;

import static org.graylog2.shared.utilities.StringUtils.f;

// TODO convert this into a resource, tools should not be simple "read operations" but perform actions
public class ListStreamsTool extends Tool<ListStreamsTool.Parameters, String> {
    public static String NAME = "list_streams";

    private final StreamService streamService;

    @Inject
    public ListStreamsTool(StreamService streamService,
                           final CustomizationConfig customizationConfig,
                           final ObjectMapper objectMapper,
                           final ClusterConfigService clusterConfigService,
                           final SchemaGeneratorProvider schemaGeneratorProvider) {
        super(
                new TypeReference<>() {},
                new TypeReference<>() {},
                NAME,
                f("List all %s Streams", customizationConfig.productName()),
                f("List all available streams in the %s instance.", customizationConfig.productName()),
                objectMapper,
                clusterConfigService,
                schemaGeneratorProvider
        );
        this.streamService = streamService;
    }

    @Override
    public String apply(PermissionHelper permissionHelper, ListStreamsTool.Parameters unused) {
        try (var dtos = streamService.streamAllDTOs()) {
            return getObjectMapper().writeValueAsString(
                    dtos.filter(stream -> permissionHelper.isPermitted(RestPermissions.STREAMS_READ, stream.getId()))
                            .map(stream -> Map.of(
                                    "id", stream.getId(),
                                    "title", stream.getTitle(),
                                    "description", stream.getDescription(),
                                    "disabled", stream.getDisabled(),
                                    "matching_type", stream.getMatchingType(),
                                    "created_at", stream.getCreatedAt(),
                                    "creator_user_id", stream.getCreatorUserId()
                            ))
                            .toList()
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Parameters {}
}
