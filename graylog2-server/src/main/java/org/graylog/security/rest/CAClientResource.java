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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.security.certutil.audit.CaAuditEventTypes;
import org.graylog.security.certutil.csr.ClientCertGenerator;
import org.graylog.security.certutil.csr.exceptions.ClientCertGenerationException;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.bootstrap.preflight.web.resources.model.CreateClientCertRequest;
import org.graylog2.plugin.rest.ApiError;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import java.io.IOException;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@Path("/ca/clientcert")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
@Api(value = "CA CLient Certificates", description = "Certificate Authority Client Certificates", tags = {CLOUD_VISIBLE})
public class CAClientResource extends RestResource {
    private final ClientCertGenerator clientCertGenerator;

    @Inject
    public CAClientResource(final ClientCertGenerator clientCertGenerator) {
        this.clientCertGenerator = clientCertGenerator;
    }

    @POST
    @AuditEvent(type = CaAuditEventTypes.CLIENTCERT_CREATE)
    @ApiOperation("Creates a client certificate")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(RestPermissions.GRAYLOG_CA_CLIENTCERT_CREATE)
    public Response createClientCert(@ApiParam(name = "request", required = true) @NotNull @Valid CreateClientCertRequest request) {
        try {
            var cert = clientCertGenerator.generateClientCert(request.principal(), request.role(), request.password().toCharArray());
            return Response.ok().entity(cert).build();
        } catch (ClientCertGenerationException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ApiError.create(e.getMessage())).build();
        }
    }

    @DELETE
    @Path("{role}/{principal}")
    @AuditEvent(type = CaAuditEventTypes.CLIENTCERT_DELETE)
    @ApiOperation("removes the cert and the user from the role")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(RestPermissions.GRAYLOG_CA_CLIENTCERT_DELETE)
    public Response deleteClientCert(@ApiParam(name = "role", required = true) @PathParam("role") String role, @ApiParam(name = "principal", required = true) @PathParam("principal") String principal) {
        try {
            clientCertGenerator.removeCertFor(role, principal);
            return Response.ok().build();
        } catch (IOException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ApiError.create(e.getMessage())).build();
        }
    }
}
