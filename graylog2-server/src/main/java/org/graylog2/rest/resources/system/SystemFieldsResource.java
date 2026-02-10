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
package org.graylog2.rest.resources.system;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Sets;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.indexer.indexset.registry.IndexSetRegistry;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.plugin.Message;
import org.graylog2.shared.rest.PublicCloudAPI;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@RequiresAuthentication
@PublicCloudAPI
@Tag(name = "System/Fields", description = "Get list of message fields that exist.")
@Path("/system/fields")
public class SystemFieldsResource extends RestResource {
    private final Indices indices;
    private final IndexSetRegistry indexSetRegistry;

    @Inject
    public SystemFieldsResource(Indices indices, IndexSetRegistry indexSetRegistry) {
        this.indices = indices;
        this.indexSetRegistry = indexSetRegistry;
    }

    private static final String FIELD_FIELDS = "fields";

    private static final Collection<String> STANDARD_FIELDS = List.of(
            Message.FIELD_SOURCE,
            Message.FIELD_MESSAGE,
            Message.FIELD_TIMESTAMP
    );

    public record MessageFieldsDTO(
            @JsonProperty(FIELD_FIELDS) Set<String> fields) {}

    @GET
    @Timed
    @Operation(summary = "Get list of message fields that exist",
                  description = "This operation is comparatively fast because it reads directly from the indexer mapping.")
    @RequiresPermissions(RestPermissions.FIELDNAMES_READ)
    @Produces(APPLICATION_JSON)
    public MessageFieldsDTO fields(@Parameter(name = "limit", description = "Maximum number of fields to return. Set to 0 for all fields.")
                                   @QueryParam("limit") int limit) {
        boolean unlimited = limit <= 0;

        final String[] writeIndexWildcards = indexSetRegistry.getIndexWildcards();

        Set<String> fields;
        if (unlimited) {
            fields = new HashSet<>();
        } else {
            fields = Sets.newHashSetWithExpectedSize(limit);
        }
        // Requesting all fields for all indices at once risks exceeding the allocated buffer, so we get fields
        // index by index instead. https://github.com/Graylog2/graylog2-server/issues/22743
        // Consider caching fields to improve performance. This would also allow for pagination.
        Stream.of(writeIndexWildcards)
                .flatMap(index -> indices.getAllMessageFields(new String[]{index}).stream())
                .distinct()
                .limit(unlimited ? Long.MAX_VALUE : limit)
                .forEach(fields::add);

        if (!unlimited) {
            fields.addAll(STANDARD_FIELDS);
        }

        return new MessageFieldsDTO(fields);
    }
}
