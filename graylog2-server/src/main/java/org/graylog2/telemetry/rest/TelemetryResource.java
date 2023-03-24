package org.graylog2.telemetry.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.rest.models.system.responses.SystemOverviewResponse;
import org.graylog2.shared.rest.resources.ProxiedResource;
import org.graylog2.shared.rest.resources.system.RemoteSystemResource;

import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Api(value = "Telemetry", description = "Message inputs", tags = {CLOUD_VISIBLE})
@Path("/telemetry")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TelemetryResource extends ProxiedResource {

    private final TelemetryService telemetryService;

    protected TelemetryResource(NodeService nodeService,
                                RemoteInterfaceProvider remoteInterfaceProvider,
                                @Context HttpHeaders httpHeaders,
                                @Named("proxiedRequestsExecutorService") ExecutorService executorService,
                                TelemetryService telemetryService) {
        super(httpHeaders, nodeService, remoteInterfaceProvider, executorService);
        this.telemetryService = telemetryService;
    }

    @GET
    @ApiOperation(value = "Get telemetry information.")
    public TelemetryResponse get() {
        return telemetryService.createTelemetryResponse(getCurrentUser(), getSystemOverviewResponses());
    }

    private Map<String, SystemOverviewResponse> getSystemOverviewResponses() {
        Map<String, SystemOverviewResponse> results = new HashMap<>();
        requestOnAllNodes(
                createRemoteInterfaceProvider(RemoteSystemResource.class),
                RemoteSystemResource::system)
                .forEach((s, r) -> results.put(s, toSystemOverviewResponse(r)));
        return results;
    }

    private SystemOverviewResponse toSystemOverviewResponse(CallResult<SystemOverviewResponse> callResult) {
        return Optional.ofNullable(callResult.response()).flatMap(NodeResponse::entity).orElse(null);
    }

}
