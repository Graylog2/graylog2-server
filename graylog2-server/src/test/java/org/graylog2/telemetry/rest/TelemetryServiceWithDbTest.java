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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.indexer.cluster.ClusterAdapter;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.users.UserService;
import org.graylog2.storage.SearchVersion;
import org.graylog2.system.traffic.TrafficCounterService;
import org.graylog2.telemetry.cluster.db.DBTelemetryClusterInfo;
import org.graylog2.telemetry.enterprise.TelemetryEnterpriseDataProvider;
import org.graylog2.telemetry.user.db.DBTelemetryUserSettingsService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class TelemetryServiceWithDbTest {

    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

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
    EventBus eventBus;
    @Mock
    User user;

    TelemetryService telemetryService;

    @Before
    public void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        MongoJackObjectMapperProvider mongoJackObjectMapperProvider = new MongoJackObjectMapperProvider(objectMapper);
        telemetryService = new TelemetryService(
                true,
                trafficCounterService,
                clusterConfigService,
                enterpriseDataProvider,
                userService,
                pluginMetaDataSet,
                elasticClusterAdapter,
                elasticsearchVersion,
                new TelemetryResponseFactory(objectMapper),
                new DBTelemetryUserSettingsService(mongodb.mongoConnection(), mongoJackObjectMapperProvider),
                new DBTelemetryClusterInfo(Duration.ZERO, mongodb.mongoConnection()),
                eventBus
        );
    }

    @Test
    public void test_no_telemetry_user_settings_present() {
        TelemetryUserSettings telemetryUserSettings = telemetryService.getTelemetryUserSettings(user);

        assertThat(telemetryUserSettings.telemetryEnabled()).isTrue();
        assertThat(telemetryUserSettings.telemetryPermissionAsked()).isFalse();
    }

    @Test
    public void test_save_telemetry_user_settings() {
        Mockito.when(user.getId()).thenReturn("id");

        telemetryService.saveUserSettings(user, TelemetryUserSettings.builder()
                .telemetryEnabled(false)
                .telemetryPermissionAsked(true)
                .build());

        TelemetryUserSettings telemetryUserSettings = telemetryService.getTelemetryUserSettings(user);

        assertThat(telemetryUserSettings.telemetryEnabled()).isFalse();
        assertThat(telemetryUserSettings.telemetryPermissionAsked()).isTrue();
    }

    @Test
    public void test_delete_telemetry_user_settings() {
        Mockito.when(user.getId()).thenReturn("id");

        telemetryService.saveUserSettings(user, TelemetryUserSettings.builder()
                .telemetryEnabled(false)
                .telemetryPermissionAsked(true)
                .build());

        telemetryService.deleteUserSettingsByUser(user);
        TelemetryUserSettings telemetryUserSettings = telemetryService.getTelemetryUserSettings(user);

        assertThat(telemetryUserSettings.telemetryEnabled()).isTrue();
        assertThat(telemetryUserSettings.telemetryPermissionAsked()).isFalse();
    }
}
