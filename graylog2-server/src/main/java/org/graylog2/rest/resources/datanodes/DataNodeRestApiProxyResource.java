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
package org.graylog2.rest.resources.datanodes;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.indexer.datanode.ProxyRequestAdapter;
import org.graylog2.shared.rest.resources.RestResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Predicate;

import static org.graylog2.audit.AuditEventTypes.DATANODE_API_REQUEST;

@RequiresAuthentication
@Api(value = "DataNodes/REST/API", description = "Proxy direct access to Data Node's API")
@Path("/datanodes/rest/{path: .*}")
@Produces(MediaType.APPLICATION_JSON)
@Timed
@RequiresPermissions("*")
public class DataNodeRestApiProxyResource extends RestResource {
    private static final List<Predicate<ProxyRequestAdapter.ProxyRequest>> allowList = List.of(
            request -> request.path().startsWith("indices-directory")
    );

    private final DatanodeRestApiProxy proxyRequestAdapter;
    private final boolean enableAllowlist;

    @Inject
    public DataNodeRestApiProxyResource(DatanodeRestApiProxy proxyRequestAdapter,
                                        @Named("datanode_proxy_api_allowlist") boolean enableAllowlist) {
        this.proxyRequestAdapter = proxyRequestAdapter;
        this.enableAllowlist = enableAllowlist;
    }

    @GET
    @ApiOperation(value = "GET request to Data Node's API")
    @AuditEvent(type = DATANODE_API_REQUEST, captureRequestEntity = false, captureResponseEntity = false)
    public Response requestGet(@ApiParam(name = "path", required = true)
                               @PathParam("path") String path,
                               @Context ContainerRequestContext requestContext) throws IOException {
        return request(requestContext.getMethod(), path, requestContext.getEntityStream());
    }

    @POST
    @ApiOperation(value = "POST request to Data Node's API")
    @AuditEvent(type = DATANODE_API_REQUEST, captureRequestEntity = false, captureResponseEntity = false)
    public Response requestPost(@ApiParam(name = "path", required = true)
                                @PathParam("path") String path,
                                @Context ContainerRequestContext requestContext) throws IOException {
        return request(requestContext.getMethod(), path, requestContext.getEntityStream());
    }

    @PUT
    @ApiOperation(value = "PUT request to Data Node's API")
    @AuditEvent(type = DATANODE_API_REQUEST, captureRequestEntity = false, captureResponseEntity = false)
    public Response requestPut(@ApiParam(name = "path", required = true)
                               @PathParam("path") String path,
                               @Context ContainerRequestContext requestContext) throws IOException {
        return request(requestContext.getMethod(), path, requestContext.getEntityStream());
    }

    @DELETE
    @ApiOperation(value = "DELETE request to Data Node's API")
    @AuditEvent(type = DATANODE_API_REQUEST, captureRequestEntity = false, captureResponseEntity = false)
    public Response requestDelete(@ApiParam(name = "path", required = true)
                                  @PathParam("path") String path,
                                  @Context ContainerRequestContext requestContext) throws IOException {
        return request(requestContext.getMethod(), path, requestContext.getEntityStream());
    }

    private Response request(String method, String path, InputStream entityStream) throws IOException {
        final var request = new ProxyRequestAdapter.ProxyRequest(method, path, entityStream);
        if (enableAllowlist && allowList.stream().noneMatch(condition -> condition.test(request))) {
            return Response.status(Response.Status.BAD_REQUEST).entity("This request is not allowed.").build();
        }

        final var response = proxyRequestAdapter.request(request);
        return Response.status(response.status()).entity(response.response()).build();
    }
}
