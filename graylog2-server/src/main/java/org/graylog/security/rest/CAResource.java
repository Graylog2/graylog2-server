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
package org.graylog.security.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.graylog.security.certutil.CaService;
import org.graylog.security.certutil.audit.CaAuditEventTypes;
import org.graylog.security.certutil.ca.exceptions.CACreationException;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.bootstrap.preflight.web.resources.model.CA;
import org.graylog2.bootstrap.preflight.web.resources.model.CreateCARequest;
import org.graylog2.plugin.rest.ApiError;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import java.net.URI;
import java.security.KeyStoreException;
import java.util.List;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@Path("/ca")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
@Api(value = "CA", description = "Certificate Authority", tags = {CLOUD_VISIBLE})
public class CAResource extends RestResource {
    private final CaService caService;
    private final String passwordSecret;

    @Inject
    public CAResource(final CaService caService,
                      final @Named("password_secret") String passwordSecret) {
        this.caService = caService;
        this.passwordSecret = passwordSecret;
    }

    @GET
    @ApiOperation("Returns the CA")
    @RequiresPermissions(RestPermissions.GRAYLOG_CA_READ)
    public CA get() throws KeyStoreStorageException {
        return caService.get();
    }

    @POST
    @Path("create")
    @AuditEvent(type = CaAuditEventTypes.CA_CREATE)
    @ApiOperation("Creates a CA")
    @RequiresPermissions(RestPermissions.GRAYLOG_CA_CREATE)
    public Response createCA(@ApiParam(name = "request", required = true) @NotNull @Valid CreateCARequest request) throws CACreationException, KeyStoreStorageException, KeyStoreException {
        final CA ca = caService.create(request.organization(), CaService.DEFAULT_VALIDITY, passwordSecret.toCharArray());
        final URI caUri = getUriBuilderToSelf()
                .path(CAResource.class)
                .build();
        return Response.created(caUri).entity(ca).build();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("upload")
    @AuditEvent(type = CaAuditEventTypes.CA_UPLOAD)
    @ApiOperation("Upload a CA")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(RestPermissions.GRAYLOG_CA_CREATE)
    public Response uploadCA(@ApiParam(name = "password") @FormDataParam("password") String password, @ApiParam(name = "files") @FormDataParam("files") List<FormDataBodyPart> files) {
        try {
            caService.upload(password, files);
            return Response.ok().build();
        } catch (CACreationException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ApiError.create(e.getMessage())).build();
        }
    }

}
