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

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
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
import org.graylog2.telemetry.db.DBTelemetryUserSettingsService;
import org.graylog2.telemetry.db.TelemetryUserSettingsDto;
import org.graylog2.telemetry.enterprise.TelemetryEnterpriseDataProvider;
import org.graylog2.users.events.UserDeletedEvent;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.graylog2.configuration.TelemetryConfiguration.TELEMETRY_ENABLED;
import static org.graylog2.shared.utilities.StringUtils.f;

public class TelemetryService {

    private static final Logger LOG = LoggerFactory.getLogger(TelemetryService.class);
    private final TrafficCounterService trafficCounterService;
    private final ClusterConfigService clusterConfigService;
    private final TelemetryEnterpriseDataProvider enterpriseDataProvider;
    private final UserService userService;
    private final Set<PluginMetaData> pluginMetaDataSet;
    private final ClusterAdapter elasticClusterAdapter;
    private final SearchVersion elasticsearchVersion;
    private final TelemetryResponseFactory telemetryResponseFactory;
    private final DBTelemetryUserSettingsService dbTelemetryUserSettingsService;
    private final boolean isTelemetryEnabled;


    @Inject
    public TelemetryService(
            @Named(TELEMETRY_ENABLED) boolean isTelemetryEnabled,
            TrafficCounterService trafficCounterService,
            ClusterConfigService clusterConfigService,
            TelemetryEnterpriseDataProvider enterpriseDataProvider,
            UserService userService,
            Set<PluginMetaData> pluginMetaDataSet,
            ClusterAdapter elasticClusterAdapter,
            @DetectedSearchVersion SearchVersion elasticsearchVersion,
            TelemetryResponseFactory telemetryResponseFactory,
            DBTelemetryUserSettingsService dbTelemetryUserSettingsService,
            EventBus eventBus) {
        this.isTelemetryEnabled = isTelemetryEnabled;
        this.trafficCounterService = trafficCounterService;
        this.clusterConfigService = clusterConfigService;
        this.enterpriseDataProvider = enterpriseDataProvider;
        this.userService = userService;
        this.pluginMetaDataSet = pluginMetaDataSet;
        this.elasticClusterAdapter = elasticClusterAdapter;
        this.elasticsearchVersion = elasticsearchVersion;
        this.telemetryResponseFactory = telemetryResponseFactory;
        this.dbTelemetryUserSettingsService = dbTelemetryUserSettingsService;
        eventBus.register(this);
    }

    public Map<String, Object> getTelemetryResponse(User currentUser, Map<String, SystemOverviewResponse> systemOverviewResponses) {
        TelemetryUserSettings telemetryUserSettings = getTelemetryUserSettings(currentUser);
        if (isTelemetryEnabled && telemetryUserSettings.telemetryEnabled()) {
            Optional<ClusterConfig> clusterConfig = getClusterConfig();
            DateTime clusterCreationDate = clusterConfig.map(ClusterConfig::lastUpdated).orElse(null);
            String clusterId = clusterConfig.map(c -> clusterConfigService.extractPayload(c.payload(), ClusterId.class).clusterId()).orElse(null);

            return telemetryResponseFactory.createTelemetryResponse(
                    getClusterInfo(clusterId, clusterCreationDate, systemOverviewResponses),
                    getUserInfo(currentUser, clusterId),
                    getPluginInfo(),
                    getSearchClusterInfo(),
                    enterpriseDataProvider.licenseStatus(),
                    telemetryUserSettings);
        } else {
            return telemetryResponseFactory.createTelemetryDisabledResponse(telemetryUserSettings);
        }
    }

    public TelemetryUserSettings getTelemetryUserSettings(User currentUser) {
        Optional<TelemetryUserSettingsDto> telemetryUserSettings = dbTelemetryUserSettingsService.findByUserId(currentUser.getId());
        return telemetryUserSettings
                .map(dto -> TelemetryUserSettings.builder()
                        .telemetryEnabled(isTelemetryEnabled && dto.telemetryEnabled())
                        .telemetryPermissionAsked(dto.telemetryPermissionAsked())
                        .build())
                .orElse(TelemetryUserSettings.builder()
                        .telemetryEnabled(isTelemetryEnabled)
                        .telemetryPermissionAsked(false)
                        .build());
    }

    public void saveUserSettings(User currentUser, TelemetryUserSettings telemetryUserSettings) {
        TelemetryUserSettingsDto.Builder builder = TelemetryUserSettingsDto.builder()
                .userId(currentUser.getId())
                .telemetryEnabled(telemetryUserSettings.telemetryEnabled())
                .telemetryPermissionAsked(telemetryUserSettings.telemetryPermissionAsked());

        dbTelemetryUserSettingsService.findByUserId(currentUser.getId()).ifPresent(dto -> builder.id(dto.id()));
        dbTelemetryUserSettingsService.save(builder.build());
    }

    public void deleteUserSettingsByUser(User currentUser) {
        deleteUserSettingsByUserId(currentUser.getId());
    }

    @Subscribe
    private void handleUserDeletedEvent(UserDeletedEvent event) {
        deleteUserSettingsByUserId(event.userId());
    }

    private void deleteUserSettingsByUserId(String userId) {
        dbTelemetryUserSettingsService.delete(userId);
    }

    private Map<String, Object> getUserInfo(User currentUser, String clusterId) {
        try {
            if (currentUser == null) {
                LOG.debug("Couldn't create user telemetry data, because no current user exists!");
                return null;
            }
            return telemetryResponseFactory.createUserInfo(
                    generateUserHash(currentUser, clusterId),
                    currentUser.isLocalAdmin(),
                    currentUser.getRoleIds().size(),
                    enterpriseDataProvider.teamsCount(currentUser.getId()));
        } catch (NoSuchAlgorithmException e) {
            LOG.debug("Couldn't create user telemetry data, because user couldn't be hashed!", e);
            return null;
        }
    }

    private Map<String, Object> getClusterInfo(String clusterId, DateTime clusterCreationDate, Map<String, SystemOverviewResponse> systemOverviewResponses) {
        return telemetryResponseFactory.createClusterInfo(
                clusterId,
                clusterCreationDate,
                systemOverviewResponses,
                getAverageLastMonthTraffic(),
                userService.loadAll().stream().filter(user -> !user.isServiceAccount()).count());
    }

    private Map<String, Object> getPluginInfo() {
        boolean isEnterprisePluginInstalled = pluginMetaDataSet.stream().anyMatch(p -> "Graylog Enterprise".equals(p.getName()));
        List<String> plugins = pluginMetaDataSet.stream().map(p -> f("%s:%s", p.getName(), p.getVersion())).toList();
        return telemetryResponseFactory.createPluginInfo(isEnterprisePluginInstalled, plugins);
    }

    private Optional<ClusterConfig> getClusterConfig() {
        return Optional.ofNullable(clusterConfigService.getRaw(ClusterId.class));
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

    private Map<String, Object> getSearchClusterInfo() {
        Map<String, NodeInfo> nodesInfo = elasticClusterAdapter.nodesInfo();
        return telemetryResponseFactory.createSearchClusterInfo(
                nodesInfo.size(),
                elasticsearchVersion.toString(),
                nodesInfo);
    }
}
