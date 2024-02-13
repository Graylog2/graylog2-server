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
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.indexer.cluster.ClusterAdapter;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.users.UserService;
import org.graylog2.storage.SearchVersion;
import org.graylog2.system.traffic.TrafficCounterService;
import org.graylog2.telemetry.cluster.TelemetryClusterService;
import org.graylog2.telemetry.enterprise.TelemetryEnterpriseDataProvider;
import org.graylog2.telemetry.enterprise.TelemetryLicenseStatus;
import org.graylog2.telemetry.user.db.DBTelemetryUserSettingsService;
import org.graylog2.telemetry.user.db.TelemetryUserSettingsDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.shared.utilities.StringUtils.f;
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

        Map<String, Object> response = telemetryService.getTelemetryResponse(user);

        assertThat(response).containsOnlyKeys(USER_TELEMETRY_SETTINGS);
    }

    @Test
    void test_telemetry_is_disabled_for_user() {
        TelemetryService telemetryService = createTelemetryService(true);
        mockUserTelemetryEnabled(false);

        Map<String, Object> response = telemetryService.getTelemetryResponse(user);

        assertThat(response).containsOnlyKeys(USER_TELEMETRY_SETTINGS);
    }

    @Test
    void test_no_telemetry_user_settings() {
        TelemetryService telemetryService = createTelemetryService(true);
        mockTrafficData(trafficCounterService);

        Map<String, Object> response = telemetryService.getTelemetryResponse(user);

        assertThatAllTelemetryDataIsPresent(response);
    }

    @Test
    void test_telemetry_enabled_for_user() {
        TelemetryService telemetryService = createTelemetryService(true);
        mockUserTelemetryEnabled(true);
        mockTrafficData(trafficCounterService);

        Map<String, Object> response = telemetryService.getTelemetryResponse(user);

        assertThatAllTelemetryDataIsPresent(response);
    }


    @Test
    void test_licenses() {
        TelemetryService telemetryService = createTelemetryService(true);
        mockUserTelemetryEnabled(true);
        mockTrafficData(trafficCounterService);
        TelemetryLicenseStatus enterpriseLicense = createLicense("/license/enterprise");
        TelemetryLicenseStatus expiredEnterpriseLicense = enterpriseLicense.toBuilder().expired(true).build();
        TelemetryLicenseStatus invalidEnterpriseLicense = enterpriseLicense.toBuilder().valid(false).build();
        TelemetryLicenseStatus olderEnterpriseLicense = enterpriseLicense.toBuilder()
                .expirationDate(enterpriseLicense.expirationDate().minusDays(1)).build();
        TelemetryLicenseStatus securityLicense = createLicense("/license/security");
        when(enterpriseDataProvider.licenseStatus()).thenReturn(List.of(
                olderEnterpriseLicense,
                invalidEnterpriseLicense,
                enterpriseLicense,
                expiredEnterpriseLicense,
                securityLicense));

        Map<String, Object> response = telemetryService.getTelemetryResponse(user);

        assertThat(response.get(LICENSE)).isEqualTo(merge(
                toMap(enterpriseLicense, "enterprise"),
                toMap(securityLicense, "security")));
    }


    @SafeVarargs
    public final Map<String, Object> merge(Map<String, Object>... values) {
        return Stream.of(values)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (v1, v2) -> v1));
    }

    private Map<String, Object> toMap(TelemetryLicenseStatus license, String licenseName) {
        return Map.of(
                f("license_%s_violated", licenseName), license.violated(),
                f("license_%s_expired", licenseName), license.expired(),
                f("license_%s_valid", licenseName), license.valid(),
                f("license_%s_expiration_date", licenseName), license.expirationDate(),
                f("license_%s_traffic_limit", licenseName), license.trafficLimit()
        );
    }

    private TelemetryLicenseStatus createLicense(String subject) {
        return TelemetryLicenseStatus.builder()
                .valid(true)
                .violated(false)
                .expired(false)
                .subject(subject)
                .expirationDate(ZonedDateTime.now())
                .trafficLimit(0L)
                .build();
    }

    private void assertThatAllTelemetryDataIsPresent(Map<String, Object> response) {
        assertThat(response).containsOnlyKeys(USER_TELEMETRY_SETTINGS, CURRENT_USER, CLUSTER, LICENSE, PLUGIN, SEARCH_CLUSTER, DATA_NODES);
    }

    private TelemetryService createTelemetryService(boolean isTelemetryEnabled) {
        return new TelemetryService(
                isTelemetryEnabled,
                trafficCounterService,
                enterpriseDataProvider,
                userService,
                pluginMetaDataSet,
                elasticClusterAdapter,
                elasticsearchVersion,
                new TelemetryResponseFactory(),
                dbTelemetryUserSettingsService,
                eventBus,
                telemetryClusterService,
                "unknown",
                nodeService);
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
