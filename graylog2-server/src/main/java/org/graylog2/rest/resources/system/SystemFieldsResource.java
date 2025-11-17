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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.plugin.Message;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import java.util.HashSet;
import java.util.Set;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Api(value = "System/Fields", description = "Get list of message fields that exist.", tags = {CLOUD_VISIBLE})
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

    public record MessageFieldsDTO(
            @JsonProperty(FIELD_FIELDS) Set<String> fields) {}

    @GET
    @Timed
    @ApiOperation(value = "Get list of message fields that exist",
                  notes = "This operation is comparatively fast because it reads directly from the indexer mapping.")
    @RequiresPermissions(RestPermissions.FIELDNAMES_READ)
    @Produces(APPLICATION_JSON)
    public MessageFieldsDTO fields(@ApiParam(name = "limit", value = "Maximum number of fields to return. Set to 0 for all fields.")
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
        int count = 0;
        outer:
        for (String wildcard : writeIndexWildcards) {
            for (String field : indices.getAllMessageFields(new String[]{wildcard})) {
                if (!unlimited && count >= limit) {
                    break outer;
                }
                fields.add(field);
                count++;
            }
        }
        if (!unlimited) {
            addStandardFields(fields);
        }

        return new MessageFieldsDTO(fields);
    }

    private void addStandardFields(Set<String> fields) {
        fields.add(Message.FIELD_SOURCE);
        fields.add(Message.FIELD_MESSAGE);
        fields.add(Message.FIELD_TIMESTAMP);
    }
}
