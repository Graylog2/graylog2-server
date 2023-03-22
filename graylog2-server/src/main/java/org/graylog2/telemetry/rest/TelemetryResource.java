package org.graylog2.telemetry.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.cluster.NodeService;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.cluster.ClusterId;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.rest.models.system.responses.SystemOverviewResponse;
import org.graylog2.shared.rest.resources.ProxiedResource;
import org.graylog2.shared.rest.resources.system.RemoteSystemResource;
import org.graylog2.system.traffic.TrafficCounterService;
import org.graylog2.telemetry.license.TelemetryLicenseStatus;
import org.graylog2.telemetry.license.TelemetryLicenseStatusProvider;
import org.joda.time.Duration;

import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;
import static org.graylog2.telemetry.rest.TelemetryResponse.ClusterInfo;
import static org.graylog2.telemetry.rest.TelemetryResponse.LicenseInfo;

@RequiresAuthentication
@Api(value = "Telemetry", description = "Message inputs", tags = {CLOUD_VISIBLE})
@Path("/telemetry")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TelemetryResource extends ProxiedResource {

    private final TrafficCounterService trafficCounterService;
    private final ClusterConfigService clusterConfigService;

    private final TelemetryLicenseStatusProvider telemetryLicenseStatusProvider;

    protected TelemetryResource(NodeService nodeService,
                                RemoteInterfaceProvider remoteInterfaceProvider,
                                @Context HttpHeaders httpHeaders,
                                @Named("proxiedRequestsExecutorService") ExecutorService executorService,
                                ClusterConfigService clusterConfigService,
                                TrafficCounterService trafficCounterService,
                                TelemetryLicenseStatusProvider telemetryLicenseStatusProvider) {
        super(httpHeaders, nodeService, remoteInterfaceProvider, executorService);
        this.trafficCounterService = trafficCounterService;
        this.clusterConfigService = clusterConfigService;
        this.telemetryLicenseStatusProvider = telemetryLicenseStatusProvider;
    }

    private static SystemOverviewResponse toSystemOverviewResponse(CallResult<SystemOverviewResponse> callResult) {
        return Optional.ofNullable(callResult.response()).flatMap(NodeResponse::entity).orElse(null);
    }

    @GET
    @ApiOperation(value = "Get telemetry information.")
    public TelemetryResponse get() {
        ClusterInfo clusterInfo = createClusterInfo();
        List<TelemetryLicenseStatus> licenses = telemetryLicenseStatusProvider.status();
        LicenseInfo licenseInfo = new LicenseInfo(licenses);
        return new TelemetryResponse(clusterInfo, licenseInfo);
    }

    private ClusterInfo createClusterInfo() {
        Map<String, SystemOverviewResponse> results = new HashMap<>();
        requestOnAllNodes(
                createRemoteInterfaceProvider(RemoteSystemResource.class),
                RemoteSystemResource::system)
                .forEach((s, r) -> results.put(s, toSystemOverviewResponse(r)));

        return new ClusterInfo(getClusterId(), results, getAverageLastMonthTraffic());
    }

    private String getClusterId() {
        return Optional.ofNullable(clusterConfigService.get(ClusterId.class)).map(ClusterId::clusterId).orElse(null);
    }

    private long getAverageLastMonthTraffic() {
        return trafficCounterService.clusterTrafficOfLastDays(Duration.standardDays(30), TrafficCounterService.Interval.DAILY)
                .output().values().stream().mapToLong(Long::longValue).sum();
    }

}
