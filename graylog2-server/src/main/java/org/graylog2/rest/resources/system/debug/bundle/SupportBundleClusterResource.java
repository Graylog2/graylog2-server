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
import okhttp3.ResponseBody;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.shared.rest.HideOnCloud;
import org.graylog2.shared.rest.resources.ProxiedResource;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static org.graylog2.shared.security.RestPermissions.SUPPORTBUNDLE_CREATE;
import static org.graylog2.shared.security.RestPermissions.SUPPORTBUNDLE_READ;
import static org.graylog2.shared.utilities.StringUtils.f;

@RequiresAuthentication
@Api(value = "Cluster/Debug/SupportBundle", description = "For collecting cluster wide debugging information, e.g. server logs")
@Path("/cluster/debug/support")
@Produces(MediaType.APPLICATION_JSON)
@HideOnCloud
public class SupportBundleClusterResource extends ProxiedResource {

    @Inject
    public SupportBundleClusterResource(NodeService nodeService,
                                        RemoteInterfaceProvider remoteInterfaceProvider,
                                        @Context HttpHeaders httpHeaders,
                                        @Named("proxiedRequestsExecutorService") ExecutorService executorService) {
        super(httpHeaders, nodeService, remoteInterfaceProvider, executorService);
    }

    @GET
    @Path("/manifest")
    @ApiOperation(value = "Get the Support Bundle Manifest from all nodes in the cluster")
    @RequiresPermissions(SUPPORTBUNDLE_READ)
    public Map<String, CallResult<SupportBundleNodeManifest>> getClusterManifest() {
        return requestOnAllNodes(RemoteSupportBundleInterface.class, RemoteSupportBundleInterface::getNodeManifest);
    }

    @POST
    @Path("/bundle/build")
    @RequiresPermissions(SUPPORTBUNDLE_CREATE)
    @ApiOperation(value = "Build a new Support Bundle")
    @Timed
    @NoAuditEvent("this is a proxy resource, the event will be triggered on the individual nodes")
    public void buildBundle(@Suspended AsyncResponse asyncResponse) {
        processAsync(asyncResponse, () -> {
                    final NodeResponse<Void> response;
                    try {
                        response = requestOnLeader(RemoteSupportBundleInterface::buildSupportBundle,
                                RemoteSupportBundleInterface.class, Duration.ofSeconds(60));
                    } catch (IOException e) {
                        return Response.serverError().entity(e.getMessage()).build();
                    }
                    return Response.status(response.code()).entity(response.body()).build();
                }
        );
    }

    @GET
    @Path("/bundle/list")
    @ApiOperation(value = "Returns the list of downloadable support bundles")
    @RequiresPermissions(SUPPORTBUNDLE_READ)
    public List<BundleFile> listBundles() throws IOException {
        final NodeResponse<List<BundleFile>> listNodeResponse = requestOnLeader(RemoteSupportBundleInterface::listBundles, RemoteSupportBundleInterface.class);
        if (listNodeResponse.isSuccess()) {
            return listNodeResponse.entity().orElse(List.of());
        }
        final String msg = f("Failed to retrieve bundle files <{}>", listNodeResponse.errorText());
        throw new ServerErrorException(msg, listNodeResponse.code());
    }

    @GET
    @Path("/bundle/download/{filename}")
    @ApiOperation(value = "Downloads the requested bundle")
    @RequiresPermissions(SUPPORTBUNDLE_READ)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response download(@PathParam("filename") @ApiParam("filename") String filename) throws IOException {
        final NodeResponse<ResponseBody> nodeResponse = requestOnLeader(c -> c.downloadBundle(filename), RemoteSupportBundleInterface.class);
        if (nodeResponse.isSuccess()) {
            // we cannot use try-with because the ResponseBody needs to stream the output
            ResponseBody responseBody = nodeResponse.entity().orElseThrow();

            try {
                StreamingOutput streamingOutput = output -> {
                    try {
                        responseBody.byteStream().transferTo(output);
                    } catch (Exception e) {
                        responseBody.close(); // avoid leaking connections on errors
                    }
                };
                var mediaType = MediaType.valueOf(MediaType.APPLICATION_OCTET_STREAM);
                Response.ResponseBuilder response = Response.ok(streamingOutput, mediaType);
                response.header("Content-Disposition", "attachment; filename=" + filename);
                return response.build();
            } catch (Exception e) {
                responseBody.close();
            }
        }
        return Response.status(nodeResponse.code()).entity(nodeResponse.body()).build();
    }

    @DELETE
    @Path("/bundle/{filename}")
    @ApiOperation(value = "Delete a certain support bundle")
    @RequiresPermissions(SUPPORTBUNDLE_CREATE)
    @NoAuditEvent("this is a proxy resource, the event will be triggered on the individual nodes")
    public Response delete(@PathParam("filename") @ApiParam("filename") String filename) throws IOException {
        final NodeResponse<Void> nodeResponse = requestOnLeader(c -> c.deleteBundle(filename), RemoteSupportBundleInterface.class);
        return Response.status(nodeResponse.code()).entity(nodeResponse.body()).build();
    }
}
