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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import org.graylog2.indexer.cluster.ClusterAdapter;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.users.UserService;
import org.graylog2.storage.SearchVersion;
import org.graylog2.system.traffic.TrafficCounterService;
import org.graylog2.telemetry.db.DBTelemetryUserSettingsService;
import org.graylog2.telemetry.db.TelemetryUserSettingsDto;
import org.graylog2.telemetry.enterprise.TelemetryEnterpriseDataProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.graylog2.system.traffic.TrafficCounterService.TrafficHistogram;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TelemetryServiceTest {

    public static final TrafficHistogram TRAFFIC_HISTOGRAM = TrafficHistogram.create(
            DateTime.now(DateTimeZone.UTC), DateTime.now(DateTimeZone.UTC), Map.of(), Map.of(), Map.of());
    private static final String CURRENT_USER = "current_user";
    private static final String USER_TELEMETRY_SETTINGS = "user_telemetry_settings";
    private static final String CLUSTER = "cluster";
    private static final String LICENSE = "license";
    private static final String PLUGIN = "plugin";
    private static final String SEARCH_CLUSTER = "search_cluster";
    @Mock
    TrafficCounterService trafficCounterService;
    @Mock
    ClusterConfigService clusterConfigService;
    @Mock
    TelemetryEnterpriseDataProvider enterpriseDataProvider;
    @Mock
    UserService userService;
    @Mock
    Set<PluginMetaData> pluginMetaDataSet;
    @Mock
    ClusterAdapter elasticClusterAdapter;
    @Mock
    SearchVersion elasticsearchVersion;
    @Mock
    ObjectMapper objectMapper;
    @Mock
    DBTelemetryUserSettingsService dbTelemetryUserSettingsService;
    @Mock
    EventBus eventBus;
    @Mock
    User user;

    @Test
    void test_telemetry_is_disabled_globally() {
        TelemetryService telemetryService = createTelemetryService(false);
        mockUserTelemetryEnabled(true);

        Map<String, Object> response = telemetryService.getTelemetryResponse(user, Map.of());

        assertThat(response).containsOnlyKeys(USER_TELEMETRY_SETTINGS);
    }

    @Test
    void test_telemetry_is_disabled_for_user() {
        TelemetryService telemetryService = createTelemetryService(true);
        mockUserTelemetryEnabled(false);

        Map<String, Object> response = telemetryService.getTelemetryResponse(user, Map.of());

        assertThat(response).containsOnlyKeys(USER_TELEMETRY_SETTINGS);
    }

    @Test
    void test_no_telemetry_user_settings() {
        TelemetryService telemetryService = createTelemetryService(true);
        when(trafficCounterService.clusterTrafficOfLastDays(any(), any())).thenReturn(TRAFFIC_HISTOGRAM);

        Map<String, Object> response = telemetryService.getTelemetryResponse(user, Map.of());

        assertThatAllTelemetryDataIsPresent(response);
    }

    @Test
    void test_telemetry_enabled_for_user() {
        TelemetryService telemetryService = createTelemetryService(true);
        mockUserTelemetryEnabled(true);
        when(trafficCounterService.clusterTrafficOfLastDays(any(), any())).thenReturn(TRAFFIC_HISTOGRAM);

        Map<String, Object> response = telemetryService.getTelemetryResponse(user, Map.of());

        assertThatAllTelemetryDataIsPresent(response);
    }

    @Test
    void test_json_serialization_error() throws JsonProcessingException {
        TelemetryService telemetryService = createTelemetryService(true, objectMapper);
        mockUserTelemetryEnabled(true);
        when(objectMapper.writeValueAsString(any())).thenThrow(JsonProcessingException.class);
        when(trafficCounterService.clusterTrafficOfLastDays(any(), any())).thenReturn(TRAFFIC_HISTOGRAM);

        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> telemetryService.getTelemetryResponse(user, Map.of()));
    }

    private void assertThatAllTelemetryDataIsPresent(Map<String, Object> response) {
        assertThat(response).containsOnlyKeys(USER_TELEMETRY_SETTINGS, CURRENT_USER, CLUSTER, LICENSE, PLUGIN, SEARCH_CLUSTER);
    }

    private TelemetryService createTelemetryService(boolean isTelemetryEnabled) {
        return createTelemetryService(isTelemetryEnabled, new ObjectMapper());
    }

    private TelemetryService createTelemetryService(boolean isTelemetryEnabled, ObjectMapper objectMapper) {
        return new TelemetryService(
                isTelemetryEnabled,
                trafficCounterService,
                clusterConfigService,
                enterpriseDataProvider,
                userService,
                pluginMetaDataSet,
                elasticClusterAdapter,
                elasticsearchVersion,
                new TelemetryResponseFactory(objectMapper),
                dbTelemetryUserSettingsService,
                eventBus
        );
    }

    private void mockUserTelemetryEnabled(boolean isTelemetryEnabled) {
        when(dbTelemetryUserSettingsService.findByUserId(any())).thenReturn(
                Optional.of(TelemetryUserSettingsDto.builder()
                        .userId("id")
                        .telemetryEnabled(isTelemetryEnabled)
                        .telemetryPermissionAsked(true)
                        .build()));
    }
}
