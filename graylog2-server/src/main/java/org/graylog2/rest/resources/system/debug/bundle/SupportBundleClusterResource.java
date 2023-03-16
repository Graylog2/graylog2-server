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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import okhttp3.ResponseBody;
import org.apache.http.HttpStatus;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.shared.rest.resources.ProxiedResource;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
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
        return requestOnAllNodes(createRemoteInterfaceProvider(RemoteSupportBundleInterface.class), RemoteSupportBundleInterface::getNodeManifest);
    }

    @POST
    @Path("/bundle/build")
    @RequiresPermissions(SUPPORTBUNDLE_CREATE)
    @NoAuditEvent("FIXME") // TODO
    public Response buildBundle() throws IOException {
        final NodeResponse<Void> voidNodeResponse = requestOnLeader(RemoteSupportBundleInterface::buildSupportBundle, createRemoteInterfaceProvider(RemoteSupportBundleInterface.class));
        if (voidNodeResponse.isSuccess()) {
            return Response.accepted().build();
        }
        return Response.serverError().build();
    }

    @GET
    @Path("/bundle/list")
    @ApiOperation(value = "Returns the list of downloadable support bundles")
    @RequiresPermissions(SUPPORTBUNDLE_READ)
    public List<BundleFile> listBundles() throws IOException {
        final NodeResponse<List<BundleFile>> listNodeResponse = requestOnLeader(RemoteSupportBundleInterface::listBundles, createRemoteInterfaceProvider(RemoteSupportBundleInterface.class));
        if (listNodeResponse.isSuccess()) {
            return listNodeResponse.entity().orElse(List.of());
        }
        throw new BadRequestException(f("Failed to retrieve bundle files <{}>", listNodeResponse.errorText()));
    }

    @GET
    @Path("/bundle/download/{filename}")
    @ApiOperation(value = "Downloads the requested bundle")
    @RequiresPermissions(SUPPORTBUNDLE_READ)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response download(@PathParam("filename") @ApiParam("filename") String filename) throws IOException {
        final NodeResponse<ResponseBody> nodeResponse = requestOnLeader(c -> c.downloadBundle(filename), createRemoteInterfaceProvider(RemoteSupportBundleInterface.class));
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
        if (nodeResponse.code() == HttpStatus.SC_NOT_FOUND) {
            return Response.status(404).build();
        }
        throw new BadRequestException("Failed to download bundle " + nodeResponse.errorText());
    }
}
