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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.eventbus.EventBus;
import org.apache.commons.collections4.IteratorUtils;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.indexer.cluster.ClusterAdapter;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.users.UserService;
import org.graylog2.storage.SearchVersion;
import org.graylog2.system.traffic.TrafficCounterService;
import org.graylog2.telemetry.cluster.TelemetryClusterService;
import org.graylog2.telemetry.user.db.DBTelemetryUserSettingsService;
import org.graylog2.telemetry.user.db.TelemetryUserSettingsDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.telemetry.rest.TelemetryTestHelper.CLUSTER;
import static org.graylog2.telemetry.rest.TelemetryTestHelper.CURRENT_USER;
import static org.graylog2.telemetry.rest.TelemetryTestHelper.DATA_NODES;
import static org.graylog2.telemetry.rest.TelemetryTestHelper.LICENSE;
import static org.graylog2.telemetry.rest.TelemetryTestHelper.PLUGIN;
import static org.graylog2.telemetry.rest.TelemetryTestHelper.SEARCH_CLUSTER;
import static org.graylog2.telemetry.rest.TelemetryTestHelper.USER_TELEMETRY_SETTINGS;
import static org.graylog2.telemetry.rest.TelemetryTestHelper.mockTrafficData;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TelemetryServiceTest {

    @Mock
    TrafficCounterService trafficCounterService;
    @Mock
    UserService userService;
    @Mock
    Set<PluginMetaData> pluginMetaDataSet;
    @Mock
    ClusterAdapter elasticClusterAdapter;
    @Mock
    SearchVersion elasticsearchVersion;
    @Mock
    DBTelemetryUserSettingsService dbTelemetryUserSettingsService;
    @Mock
    TelemetryClusterService telemetryClusterService;
    @Mock
    EventBus eventBus;
    @Mock
    User user;
    @Mock
    NodeService nodeService;

    @Test
    void test_telemetry_is_disabled_globally() {
        TelemetryService telemetryService = createTelemetryService(false);
        mockUserTelemetryEnabled(true);

        ObjectNode telemetryResponse = telemetryService.getTelemetryResponse(user);

        assertThat(IteratorUtils.toList(telemetryResponse.fieldNames())).containsOnly(USER_TELEMETRY_SETTINGS);
    }

    @Test
    void test_telemetry_is_disabled_for_user() {
        TelemetryService telemetryService = createTelemetryService(true);
        mockUserTelemetryEnabled(false);

        ObjectNode telemetryResponse = telemetryService.getTelemetryResponse(user);

        assertThat(IteratorUtils.toList(telemetryResponse.fieldNames())).containsOnly(USER_TELEMETRY_SETTINGS);
    }

    @Test
    void test_no_telemetry_user_settings() {
        TelemetryService telemetryService = createTelemetryService(true);
        mockTrafficData(trafficCounterService);

        ObjectNode telemetryResponse = telemetryService.getTelemetryResponse(user);

        assertThatAllTelemetryDataIsPresent(IteratorUtils.toList(telemetryResponse.fieldNames()));
    }

    @Test
    void test_telemetry_enabled_for_user() {
        TelemetryService telemetryService = createTelemetryService(true);
        mockUserTelemetryEnabled(true);
        mockTrafficData(trafficCounterService);

        ObjectNode telemetryResponse = telemetryService.getTelemetryResponse(user);

        assertThatAllTelemetryDataIsPresent(IteratorUtils.toList(telemetryResponse.fieldNames()));
    }

    private void assertThatAllTelemetryDataIsPresent(List<String> response) {
        assertThat(response).containsOnly(USER_TELEMETRY_SETTINGS, CURRENT_USER, CLUSTER, LICENSE, PLUGIN, SEARCH_CLUSTER, DATA_NODES);
    }

    private TelemetryService createTelemetryService(boolean isTelemetryEnabled) {
        return new TelemetryService(
                isTelemetryEnabled,
                trafficCounterService,
                userService,
                pluginMetaDataSet,
                elasticClusterAdapter,
                elasticsearchVersion,
                new TelemetryResponseFactory(new ObjectMapperProvider().get()),
                dbTelemetryUserSettingsService,
                eventBus,
                telemetryClusterService,
                "unknown",
                nodeService,
                false,
                Set.of());
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
