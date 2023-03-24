package org.graylog2.telemetry;

import com.google.common.hash.HashCode;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.cluster.ClusterId;
import org.graylog2.plugin.database.users.User;
import org.graylog2.rest.models.system.responses.SystemOverviewResponse;
import org.graylog2.system.traffic.TrafficCounterService;
import org.graylog2.telemetry.enterprise.TelemetryEnterpriseDataProvider;
import org.graylog2.telemetry.enterprise.TelemetryLicenseStatus;
import org.graylog2.telemetry.rest.TelemetryResponse;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.graylog2.shared.utilities.StringUtils.f;
import static org.graylog2.telemetry.rest.TelemetryResponse.ClusterInfo;
import static org.graylog2.telemetry.rest.TelemetryResponse.LicenseInfo;
import static org.graylog2.telemetry.rest.TelemetryResponse.UserInfo;

public class TelemetryService {

    private static final Logger LOG = LoggerFactory.getLogger(TelemetryService.class);

    private final TrafficCounterService trafficCounterService;
    private final ClusterConfigService clusterConfigService;
    private final TelemetryEnterpriseDataProvider enterpriseDataProvider;


    @Inject
    public TelemetryService(TrafficCounterService trafficCounterService,
                            ClusterConfigService clusterConfigService,
                            TelemetryEnterpriseDataProvider enterpriseDataProvider) {
        this.trafficCounterService = trafficCounterService;
        this.clusterConfigService = clusterConfigService;
        this.enterpriseDataProvider = enterpriseDataProvider;
    }

    public TelemetryResponse createTelemetryResponse(User currentUser, Map<String, SystemOverviewResponse> systemOverviewResponses) {
        String clusterId = getClusterId();

        List<TelemetryLicenseStatus> licenses = enterpriseDataProvider.licenseStatus();
        LicenseInfo licenseInfo = new LicenseInfo(licenses);
        return new TelemetryResponse(
                createUserInfo(currentUser, clusterId),
                createClusterInfo(clusterId, systemOverviewResponses),
                licenseInfo);
    }

    private UserInfo createUserInfo(User currentUser, String clusterId) {
        try {
            if (currentUser == null) {
                LOG.debug("Couldn't create user telemetry data, because no current user exists!");
                return null;
            }
            return new UserInfo(
                    generateUserHash(currentUser, clusterId),
                    currentUser.isLocalAdmin(),
                    currentUser.getRoleIds().size(),
                    enterpriseDataProvider.teamsCount(currentUser.getId()));
        } catch (NoSuchAlgorithmException e) {
            LOG.debug("Couldn't create user telemetry data, because user couldn't be hashed!", e);
            return null;
        }
    }

    private ClusterInfo createClusterInfo(String clusterId, Map<String, SystemOverviewResponse> systemOverviewResponses) {
        return new ClusterInfo(clusterId, systemOverviewResponses, getAverageLastMonthTraffic());
    }

    private String getClusterId() {
        return Optional.ofNullable(clusterConfigService.get(ClusterId.class)).map(ClusterId::clusterId).orElse(null);
    }

    private long getAverageLastMonthTraffic() {
        return trafficCounterService.clusterTrafficOfLastDays(Duration.standardDays(30), TrafficCounterService.Interval.DAILY)
                .output().values().stream().mapToLong(Long::longValue).sum();
    }

    private String generateUserHash(User currentUser, String clusterId) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(f("%s%s", currentUser.getId(), clusterId).getBytes(StandardCharsets.UTF_8));
        return HashCode.fromBytes(messageDigest.digest()).toString();
    }
}
