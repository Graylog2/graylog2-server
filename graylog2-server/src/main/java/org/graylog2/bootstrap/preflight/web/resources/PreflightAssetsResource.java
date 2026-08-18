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
package org.graylog2.bootstrap.preflight.web.resources;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.bootstrap.preflight.PreflightConstants;
import org.graylog2.bootstrap.preflight.PreflightWebModule;
import org.graylog2.web.resources.ResourceFileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimetypesFileTypeMap;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.MoreObjects.firstNonNull;

@Path("/")
public class PreflightAssetsResource {
    private static final Logger LOG = LoggerFactory.getLogger(PreflightAssetsResource.class);

    private final MimetypesFileTypeMap mimeTypes;
    private final ResourceFileReader resourceFileReader;

    @Inject
    public PreflightAssetsResource(MimetypesFileTypeMap mimeTypes, ResourceFileReader resourceFileReader) {
        this.mimeTypes = mimeTypes;
        this.resourceFileReader = resourceFileReader;
    }

    @Produces(MediaType.TEXT_HTML)
    @GET
    @RequiresPermissions(PreflightWebModule.PERMISSION_PREFLIGHT_ONLY)
    public Response index(@Context Request request) {
        return this.get(request, "index.html");
    }

    @Path("/{filename}")
    @GET
    @RequiresPermissions(PreflightWebModule.PERMISSION_PREFLIGHT_ONLY)
    public Response get(@Context Request request, @PathParam("filename") String filename) {
        try {
            final var resource = resourceFileReader.readFileFrom(PreflightConstants.ASSETS_RESOURCE_DIR, filename, getClass());
            return getResponse(request, filename, resource);
        } catch (IOException | URISyntaxException e) {
            LOG.debug("Couldn't serve preflight asset <{}>.", filename, e);
            // Don't reflect the requested file name back to the client.
            throw new NotFoundException("Couldn't find the requested resource.", e);
        }
    }

    private Response getResponse(Request request, String filename, ResourceFileReader.ResourceFile resource) {
        final byte[] fileContents = resource.contents().get();
        final Date lastModified = resource.lastModified().orElseGet(Date::new);
        final EntityTag entityTag = resource.entityTag().get();

        final Response.ResponseBuilder response = request.evaluatePreconditions(lastModified, entityTag);
        if (response != null) {
            return response.build();
        }

        final String contentType = firstNonNull(mimeTypes.getContentType(filename), MediaType.APPLICATION_OCTET_STREAM);
        final CacheControl cacheControl = new CacheControl();
        cacheControl.setMaxAge((int) TimeUnit.DAYS.toSeconds(365));
        cacheControl.setNoCache(false);
        cacheControl.setPrivate(false);

        return Response
                .ok(fileContents, contentType)
                .tag(entityTag)
                .cacheControl(cacheControl)
                .lastModified(lastModified)
                .build();
    }
}
