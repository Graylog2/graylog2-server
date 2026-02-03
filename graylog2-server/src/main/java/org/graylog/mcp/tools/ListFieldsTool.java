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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.graylog.mcp.server.SchemaGeneratorProvider;
import org.graylog.mcp.server.Tool;
import org.graylog.plugins.views.search.rest.MappedFieldTypeDTO;
import org.graylog2.indexer.fieldtypes.MappedFieldTypesService;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;

import java.util.Set;
import java.util.stream.Collectors;

public class ListFieldsTool extends Tool<ListFieldsTool.Parameters, ListFieldsTool.Result> {

    public static final String NAME = "list_fields";
    private final MappedFieldTypesService mappedFieldTypesService;

    @Inject
    protected ListFieldsTool(MappedFieldTypesService mappedFieldTypesService,
                             final ObjectMapper objectMapper,
                             final ClusterConfigService clusterConfigService,
                             final SchemaGeneratorProvider schemaGeneratorProvider) {
        super(
                new TypeReference<>() {},
                new TypeReference<>() {},
                NAME,
              "List available fields",
                """
                Retrieve the available field names and metadata about them, for example their datatype and capabilities.
                Field names must be used to properly create search queries and request specific fields from tools like
                search_messages and aggregate_messages.
                Fields can have different meanings in different streams (and also per source), so that needs to be taken into account.
                This tool can be scoped to streams to cut down on the noise.
                Fields can have various properties, such as "enumerable" which means you can aggregate on them, they can be "numeric",
                making them suitable to use in aggregation metrics, or "full-text-search", which means their content is tokenized
                """,
                objectMapper,
                clusterConfigService,
                schemaGeneratorProvider
        );
        this.mappedFieldTypesService = mappedFieldTypesService;
    }

    @Override
    protected Result apply(final PermissionHelper permissionHelper, final ListFieldsTool.Parameters parameters) {
        final Set<String> messageStreamIds = permissionHelper.getSearchUser().streams().loadAllMessageStreams();
        Set<String> filteredMessageStreamIds = messageStreamIds;
        if (parameters.streams() != null && !parameters.streams().isEmpty()) {
            filteredMessageStreamIds = messageStreamIds.stream()
                    .filter(id -> parameters.streams().contains(id))
                    .collect(Collectors.toSet());
        }
        return new Result(mappedFieldTypesService.fieldTypesByStreamIds(filteredMessageStreamIds, RelativeRange.allTime()));
    }

    // we can't return a Set directly but need a wrapper object
    public record Result(@JsonProperty("fields") Set<MappedFieldTypeDTO> fields) {}

    @AutoValue
    @JsonDeserialize(builder = Parameters.Builder.class)
    public static abstract class Parameters {

        @JsonProperty("streams")
        @JsonPropertyDescription("A set of stream IDs to get the fields for. Leave this empty to retrieve fields for all accessible streams (can be large).")
        @Nullable
        public abstract Set<String> streams();

        public static Builder builder() {
            return Builder.create();
        }

        @AutoValue.Builder
        public static abstract class Builder {

            @JsonCreator
            public static Builder create(){
                return new AutoValue_ListFieldsTool_Parameters.Builder()
                        .streams(Set.of());
            }

            @JsonProperty("streams")
            public abstract Builder streams(@Nullable final Set<String> streams);

            public abstract Parameters build();
        }
    }
}
