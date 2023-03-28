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
package org.graylog2.telemetry.rest;

import com.google.common.hash.HashCode;
import org.graylog2.cluster.ClusterConfig;
import org.graylog2.indexer.cluster.ClusterAdapter;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.cluster.ClusterId;
import org.graylog2.plugin.database.users.User;
import org.graylog2.rest.models.system.responses.SystemOverviewResponse;
import org.graylog2.shared.users.UserService;
import org.graylog2.storage.DetectedSearchVersion;
import org.graylog2.storage.SearchVersion;
import org.graylog2.system.stats.elasticsearch.NodeInfo;
import org.graylog2.system.traffic.TrafficCounterService;
import org.graylog2.telemetry.enterprise.TelemetryEnterpriseDataProvider;
import org.joda.time.DateTime;
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
import java.util.Set;

import static org.graylog2.shared.utilities.StringUtils.f;
import static org.graylog2.telemetry.rest.TelemetryResponse.ClusterInfo;
import static org.graylog2.telemetry.rest.TelemetryResponse.LicenseInfo;
import static org.graylog2.telemetry.rest.TelemetryResponse.PluginInfo;
import static org.graylog2.telemetry.rest.TelemetryResponse.SearchClusterInfo;
import static org.graylog2.telemetry.rest.TelemetryResponse.UserInfo;

public class TelemetryService {

    private static final Logger LOG = LoggerFactory.getLogger(TelemetryService.class);

    private final TrafficCounterService trafficCounterService;
    private final ClusterConfigService clusterConfigService;
    private final TelemetryEnterpriseDataProvider enterpriseDataProvider;
    private final UserService userService;
    private final Set<PluginMetaData> pluginMetaDataSet;

    private final ClusterAdapter elasticClusterAdapter;
    private final SearchVersion elasticsearchVersion;


    @Inject
    public TelemetryService(TrafficCounterService trafficCounterService,
                            ClusterConfigService clusterConfigService,
                            TelemetryEnterpriseDataProvider enterpriseDataProvider,
                            UserService userService,
                            Set<PluginMetaData> pluginMetaDataSet,
                            ClusterAdapter elasticClusterAdapter,
                            @DetectedSearchVersion SearchVersion elasticsearchVersion) {
        this.trafficCounterService = trafficCounterService;
        this.clusterConfigService = clusterConfigService;
        this.enterpriseDataProvider = enterpriseDataProvider;
        this.userService = userService;
        this.pluginMetaDataSet = pluginMetaDataSet;
        this.elasticClusterAdapter = elasticClusterAdapter;
        this.elasticsearchVersion = elasticsearchVersion;
    }

    public TelemetryResponse createTelemetryResponse(User currentUser, Map<String, SystemOverviewResponse> systemOverviewResponses) {
        String clusterId = getClusterId();

        return new TelemetryResponse(
                createUserInfo(currentUser, clusterId),
                createClusterInfo(clusterId, systemOverviewResponses),
                new LicenseInfo(enterpriseDataProvider.licenseStatus()),
                createPluginInfo(),
                createSearchClusterInfo());
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
        return new ClusterInfo(
                clusterId,
                getClusterCreationDate(),
                systemOverviewResponses.size(),
                systemOverviewResponses,
                getAverageLastMonthTraffic(),
                userService.loadAll().stream().filter(user -> !user.isServiceAccount()).count());
    }

    private PluginInfo createPluginInfo() {
        boolean isEnterprisePluginInstalled = pluginMetaDataSet.stream().anyMatch(p -> "Graylog Enterprise".equals(p.getName()));
        List<String> plugins = pluginMetaDataSet.stream().map(p -> f("%s:%s", p.getName(), p.getVersion())).toList();
        return new PluginInfo(isEnterprisePluginInstalled, plugins);
    }

    private String getClusterId() {
        return Optional.ofNullable(clusterConfigService.get(ClusterId.class)).map(ClusterId::clusterId).orElse(null);
    }

    private DateTime getClusterCreationDate() {
        return Optional.ofNullable(clusterConfigService.getRaw(ClusterId.class)).map(ClusterConfig::lastUpdated).orElse(null);
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

    private SearchClusterInfo createSearchClusterInfo() {
        Map<String, NodeInfo> nodesInfo = elasticClusterAdapter.nodesInfo();
        return new SearchClusterInfo(
                nodesInfo.size(),
                elasticsearchVersion.toString(),
                nodesInfo);
    }
}
