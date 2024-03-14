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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
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
import org.graylog.security.certutil.csr.ClientCertGenerator;
import org.graylog.security.certutil.csr.exceptions.ClientCertGenerationException;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.bootstrap.preflight.web.resources.model.CA;
import org.graylog2.bootstrap.preflight.web.resources.model.CreateCARequest;
import org.graylog2.bootstrap.preflight.web.resources.model.CreateClientCertRequest;
import org.graylog2.plugin.rest.ApiError;

import java.io.IOException;
import java.security.KeyStoreException;
import java.util.List;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@Path("/ca")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
@Api(value = "CA", description = "Certificate Authority", tags = {CLOUD_VISIBLE})
public class CAResource {
    private final CaService caService;
    private final String passwordSecret;
    private final ClientCertGenerator clientCertGenerator;

    @Inject
    public CAResource(final CaService caService,
                      final @Named("password_secret") String passwordSecret,
                      final ClientCertGenerator clientCertGenerator) {
        this.caService = caService;
        this.passwordSecret = passwordSecret;
        this.clientCertGenerator = clientCertGenerator;
    }

    @GET
    @ApiOperation("Returns the CA")
    public CA get() throws KeyStoreStorageException {
        return caService.get();
    }

    @POST
    @Path("create")
    @AuditEvent(type = CaAuditEventTypes.CA_CREATE)
    @ApiOperation("Creates a CA")
    public void createCA(@ApiParam(name = "request", required = true) @NotNull @Valid CreateCARequest request) throws CACreationException, KeyStoreStorageException, KeyStoreException {
        caService.create(request.organization(), CaService.DEFAULT_VALIDITY, passwordSecret.toCharArray());
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("upload")
    @AuditEvent(type = CaAuditEventTypes.CA_UPLOAD)
    @ApiOperation("Upload a CA")
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadCA(@ApiParam(name = "password") @FormDataParam("password") String password, @ApiParam(name = "files") @FormDataParam("files") List<FormDataBodyPart> files) {
        try {
            caService.upload(password, files);
            return Response.ok().build();
        } catch (CACreationException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ApiError.create(e.getMessage())).build();
        }
    }

    @POST
    @Path("clientcert")
    @AuditEvent(type = CaAuditEventTypes.CLIENTCERT_CREATE)
    @ApiOperation("Creates a client certificate")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("*")
    public Response createClientCert(@ApiParam(name = "request", required = true) @NotNull @Valid CreateClientCertRequest request) {
        try {
            var cert = clientCertGenerator.generateClientCert(request.principal(), request.role(), request.password().toCharArray());
            return Response.ok().entity(cert).build();
        } catch (ClientCertGenerationException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ApiError.create(e.getMessage())).build();
        }
    }

    @DELETE
    @Path("clientcert/{role}/{principal}")
    @AuditEvent(type = CaAuditEventTypes.CLIENTCERT_DELETE)
    @ApiOperation("removes the cert and the user from the role")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("*")
    public Response deleteClientCert(@ApiParam(name = "role", required = true) @PathParam("role") String role, @ApiParam(name = "principal", required = true) @PathParam("principal") String principal) {
        try {
            clientCertGenerator.removeCertFor(role, principal);
            return Response.ok().build();
        } catch (IOException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ApiError.create(e.getMessage())).build();
        }
    }
}
