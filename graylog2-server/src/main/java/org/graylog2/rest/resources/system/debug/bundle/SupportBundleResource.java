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
package org.graylog2.rest.resources.system.debug.bundle;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestrictToLeader;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.graylog2.shared.security.RestPermissions.SUPPORTBUNDLE_CREATE;
import static org.graylog2.shared.security.RestPermissions.SUPPORTBUNDLE_READ;
import static org.graylog2.shared.utilities.StringUtils.f;

@RequiresAuthentication
@Api(value = "System/Debug/SupportBundle", description = "For collecting debugging information, e.g. server logs")
@Path("/system/debug/support")
@Produces(MediaType.APPLICATION_JSON)
public class SupportBundleResource extends RestResource {
    private final NodeId nodeId;
    private final SupportBundleService supportBundleService;

    @Inject
    public SupportBundleResource(NodeId nodeId, SupportBundleService supportBundleService) {
        this.nodeId = checkNotNull(nodeId);
        this.supportBundleService = supportBundleService;
    }

    @GET
    @Path("/manifest")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get a nodes' Support Bundle Manifest")
    @RequiresPermissions(SUPPORTBUNDLE_READ)
    public SupportBundleNodeManifest getNodeManifest() {
        return supportBundleService.getManifest();
    }

    @GET
    @Path("/logfile/{id}")
    @ApiOperation(value = "Retrieve the nodes' server logfile")
    @RequiresPermissions(SUPPORTBUNDLE_READ)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLogFile(@PathParam("id") @ApiParam(name = "id", value = "The id of the logfile as referenced from the Support Bundle Manifest") String id) {

        final Optional<LogFile> logFileOptional = supportBundleService.getManifest().entries().logfiles().stream().filter(l -> l.id().equals(id)).findFirst();
        var logFile = logFileOptional.orElseThrow(() -> new NotFoundException(f("No logfile found for id <%s>", id)));

        var mediaType = MediaType.valueOf(MediaType.APPLICATION_OCTET_STREAM);
        StreamingOutput streamingOutput = outputStream -> supportBundleService.loadLogFileStream(logFile, outputStream);
        Response.ResponseBuilder response = Response.ok(streamingOutput, mediaType);
        response.header("Content-Disposition", "attachment; filename=" + logFile.name());
        response.header("Content-Length", logFile.size());
        return response.build();
    }

    @POST
    @Path("/bundle/build")
    @Timed
    @RestrictToLeader
    @RequiresPermissions(SUPPORTBUNDLE_CREATE)
    @Produces(MediaType.APPLICATION_JSON)
    @NoAuditEvent("FIXME") // TODO
    public Response buildBundle(@Context HttpHeaders httpHeaders) {
        supportBundleService.buildBundle(httpHeaders, getSubject());
        return Response.accepted().build();
    }
}
