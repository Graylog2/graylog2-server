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
package org.graylog2.rest.resources.streams.favoritefields;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.shared.rest.PublicCloudAPI;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.FavoriteFieldsService;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RequiresAuthentication
@PublicCloudAPI
@Tag(name = "FavoriteFields", description = "Retrieve/set favorite fields per stream")
@Produces(MediaType.APPLICATION_JSON)
@Path("/favorite_fields")
public class FavoriteFieldsResource extends RestResource {
    private final FavoriteFieldsService favoriteFieldsService;

    @Inject
    public FavoriteFieldsResource(FavoriteFieldsService favoriteFieldsService) {
        this.favoriteFieldsService = favoriteFieldsService;
    }

    public record SetFavoriteFieldsRequest(Map<String, List<String>> fields) {}

    @POST
    @Timed
    @Operation(summary = "Set favorite fields for a list of streams")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @AuditEvent(type = AuditEventTypes.FAVORITE_FIELDS_UPDATE)
    public void set(@RequestBody(required = true) final SetFavoriteFieldsRequest request) throws ValidationException {
        request.fields().keySet().forEach(streamId -> checkPermission(RestPermissions.STREAMS_EDIT, streamId));

        request.fields().forEach(favoriteFieldsService::set);
    }

    public record FavoriteFieldRequest(@JsonProperty("stream_ids") @NotNull Set<String> streamIds,
                                       @JsonProperty("field") @NotBlank String field) {}

    @PATCH
    @Timed
    @Operation(summary = "Add favorite field for a list of streams")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @AuditEvent(type = AuditEventTypes.FAVORITE_FIELDS_UPDATE)
    @Path("/add")
    public void add(@RequestBody(required = true) @Valid final FavoriteFieldRequest request) throws ValidationException {
        request.streamIds().forEach(streamId -> checkPermission(RestPermissions.STREAMS_EDIT, streamId));

        request.streamIds().forEach(streamId -> favoriteFieldsService.add(streamId, request.field()));
    }

    @PATCH
    @Timed
    @Operation(summary = "Remove favorite field from a list of streams")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @AuditEvent(type = AuditEventTypes.FAVORITE_FIELDS_UPDATE)
    @Path("/remove")
    public void remove(@RequestBody(required = true) @Valid final FavoriteFieldRequest request) throws ValidationException {
        request.streamIds().forEach(streamId -> checkPermission(RestPermissions.STREAMS_EDIT, streamId));

        request.streamIds().forEach(streamId -> favoriteFieldsService.remove(streamId, request.field()));
    }
}
