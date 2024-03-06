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
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.shared.rest.HideOnCloud;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestrictToLeader;

import jakarta.inject.Inject;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.List;
import java.util.Optional;

import static org.graylog2.shared.security.RestPermissions.SUPPORTBUNDLE_CREATE;
import static org.graylog2.shared.security.RestPermissions.SUPPORTBUNDLE_READ;
import static org.graylog2.shared.utilities.StringUtils.f;

@RequiresAuthentication
@Api(value = "System/Debug/SupportBundle", description = "For collecting debugging information, e.g. server logs")
@Path("/system/debug/support")
@Produces(MediaType.APPLICATION_JSON)
@HideOnCloud
public class SupportBundleResource extends RestResource {
    private final SupportBundleService supportBundleService;

    @Inject
    public SupportBundleResource(SupportBundleService supportBundleService) {
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
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
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
    @AuditEvent(type = AuditEventTypes.SUPPORT_BUNDLE_CREATE)
    public Response buildBundle(@Context HttpHeaders httpHeaders) {
        supportBundleService.buildBundle(httpHeaders, getSubject());
        return Response.accepted().build();
    }

    @GET
    @Path("/bundle/list")
    @ApiOperation(value = "Returns the list of downloadable support bundles")
    @RequiresPermissions(SUPPORTBUNDLE_READ)
    @RestrictToLeader
    public List<BundleFile> listBundles() {
        return supportBundleService.listBundles();
    }

    @GET
    @Path("/bundle/download/{filename}")
    @ApiOperation(value = "Downloads the requested bundle")
    @RequiresPermissions(SUPPORTBUNDLE_READ)
    @RestrictToLeader
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @AuditEvent(type = AuditEventTypes.SUPPORT_BUNDLE_DOWNLOAD)
    public Response download(@PathParam("filename") @ApiParam("filename") String filename) {
        var mediaType = MediaType.valueOf(MediaType.APPLICATION_OCTET_STREAM);
        StreamingOutput streamingOutput = outputStream -> supportBundleService.downloadBundle(filename, outputStream);
        Response.ResponseBuilder response = Response.ok(streamingOutput, mediaType);
        response.header("Content-Disposition", "attachment; filename=" + filename);
        return response.build();
    }

    @DELETE
    @Path("/bundle/{filename}")
    @ApiOperation(value = "Delete a certain support bundle")
    @RequiresPermissions(SUPPORTBUNDLE_CREATE)
    @RestrictToLeader
    @AuditEvent(type = AuditEventTypes.SUPPORT_BUNDLE_DELETE)
    public Response delete(@PathParam("filename") @ApiParam("filename") String filename) throws IOException {
        try {
            supportBundleService.deleteBundle(filename);
        } catch (NoSuchFileException e) {
            throw new NotFoundException(e);
        }
        return Response.accepted().build();
    }
}
