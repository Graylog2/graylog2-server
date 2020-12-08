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
package org.graylog.plugins.views.search.rest;

import com.google.common.collect.ImmutableSet;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.indexer.fieldtypes.MappedFieldTypesService;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.exceptions.MissingStreamPermissionException;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Set;
import java.util.stream.Collectors;

@Api(value = "Field Types")
@Path("/views/fields")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class FieldTypesResource extends RestResource implements PluginRestResource {
    private static final Logger LOG = LoggerFactory.getLogger(FieldTypesResource.class);
    private final MappedFieldTypesService mappedFieldTypesService;
    private final PermittedStreams permittedStreams;

    @Inject
    public FieldTypesResource(MappedFieldTypesService mappedFieldTypesService,
                              PermittedStreams permittedStreams) {
        this.mappedFieldTypesService = mappedFieldTypesService;
        this.permittedStreams = permittedStreams;
    }

    @GET
    @ApiOperation(value = "Retrieve the list of all fields present in the system")
    public Set<MappedFieldTypeDTO> allFieldTypes() {
        return mappedFieldTypesService.fieldTypesByStreamIds(permittedStreams.load(this::allowedToReadStream));
    }

    private boolean allowedToReadStream(String streamId) {
        return isPermitted(RestPermissions.STREAMS_READ, streamId);
    }

    @POST
    @ApiOperation(value = "Retrieve the field list of a given set of streams")
    @NoAuditEvent("This is not changing any data")
    public Set<MappedFieldTypeDTO> byStreams(FieldTypesForStreamsRequest request) {
        checkStreamPermission(request.streams());

        return mappedFieldTypesService.fieldTypesByStreamIds(request.streams());
    }

    private void checkStreamPermission(Set<String> streamIds) {
        Set<String> notPermittedStreams = streamIds.stream().filter(s -> !isPermitted(RestPermissions.STREAMS_READ, s))
                .collect(Collectors.toSet());
        if (!notPermittedStreams.isEmpty()) {
            LOG.info("Not authorized to access resource id <{}>. User <{}> is missing permission <{}:{}>",
                    streamIds, getSubject().getPrincipal(), RestPermissions.STREAMS_READ, streamIds);
            throw new MissingStreamPermissionException("Not authorized to access streams.",
                    streamIds);
        }
    }
}
