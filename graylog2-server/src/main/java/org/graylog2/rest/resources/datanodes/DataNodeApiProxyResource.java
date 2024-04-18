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
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.indexer.datanode.ProxyRequestAdapter;
import org.graylog2.shared.rest.resources.RestResource;

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
import org.graylog2.shared.security.RestPermissions;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;

import static org.graylog2.audit.AuditEventTypes.DATANODE_API_REQUEST;

@RequiresAuthentication
@Api(value = "DataNodes/API", description = "Proxy direct access to Data Node's API")
@Produces(MediaType.APPLICATION_JSON)
@Timed
@Path("/datanodes/{hostname}/opensearch/{path: .*}")
@RequiresPermissions(RestPermissions.DATANODE_OPENSEARCH_PROXY)
public class DataNodeApiProxyResource extends RestResource {
    private static final List<Predicate<ProxyRequestAdapter.ProxyRequest>> allowList = List.of(
            request -> request.path().startsWith("_cluster"),
            request -> request.path().startsWith("_cat"),
            request -> request.path().startsWith("_mapping") && request.method().equals("GET")
    );

    private final ProxyRequestAdapter proxyRequestAdapter;
    private final boolean enableAllowlist;

    @Inject
    public DataNodeApiProxyResource(ProxyRequestAdapter proxyRequestAdapter,
                                    @Named("datanode_proxy_api_allowlist") boolean enableAllowlist) {
        this.proxyRequestAdapter = proxyRequestAdapter;
        this.enableAllowlist = enableAllowlist;
    }

    @GET
    @ApiOperation(value = "GET request to Data Node's API")
    @AuditEvent(type = DATANODE_API_REQUEST)
    public Response requestGet(@ApiParam(name = "path", required = true)
                                       @PathParam("path") String path,
                                       @ApiParam(name = "hostname", required = true)
                                       @PathParam("hostname") String hostname,
                                       @Context ContainerRequestContext requestContext) throws IOException {
        return request(requestContext, path, hostname);
    }

    @POST
    @ApiOperation(value = "POST request to Data Node's API")
    @AuditEvent(type = DATANODE_API_REQUEST)
    public Response requestPost(@ApiParam(name = "path", required = true)
                                        @PathParam("path") String path,
                                        @ApiParam(name = "hostname", required = true)
                                        @PathParam("hostname") String hostname,
                                        @Context ContainerRequestContext requestContext) throws IOException {
        return request(requestContext, path, hostname);
    }

    @PUT
    @ApiOperation(value = "PUT request to Data Node's API")
    @AuditEvent(type = DATANODE_API_REQUEST)
    public Response requestPut(@ApiParam(name = "path", required = true)
                               @PathParam("path") String path,
                               @ApiParam(name = "hostname", required = true)
                               @PathParam("hostname") String hostname,
                               @Context ContainerRequestContext requestContext) throws IOException {
        return request(requestContext, path, hostname);
    }

    @DELETE
    @ApiOperation(value = "DELETE request to Data Node's API")
    @AuditEvent(type = DATANODE_API_REQUEST)
    public Response requestDelete(@ApiParam(name = "path", required = true)
                                  @PathParam("path") String path,
                                  @ApiParam(name = "hostname", required = true)
                                  @PathParam("hostname") String hostname,
                                  @Context ContainerRequestContext requestContext) throws IOException {
        return request(requestContext, path, hostname);
    }

    private Response request(ContainerRequestContext context, String path, String hostname) throws IOException {
        final var request = new ProxyRequestAdapter.ProxyRequest(context.getMethod(), path, context.getEntityStream(), hostname, context.getUriInfo().getQueryParameters());

        if (enableAllowlist && allowList.stream().noneMatch(condition -> condition.test(request))) {
            return Response.status(Response.Status.BAD_REQUEST).entity("This request is not allowed.").build();
        }

        final var response = proxyRequestAdapter.request(request);
        return Response.status(response.status()).type(response.contentType()).entity(response.response()).build();
    }
}
