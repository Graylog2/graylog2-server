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
import com.github.wnameless.json.flattener.JsonFlattener;
import org.graylog2.system.stats.elasticsearch.NodeInfo;
import org.graylog2.telemetry.enterprise.TelemetryLicenseStatus;
import org.joda.time.DateTime;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.graylog2.shared.utilities.StringUtils.f;
import static org.graylog2.telemetry.cluster.db.DBTelemetryClusterInfo.FIELD_IS_LEADER;
import static org.graylog2.telemetry.cluster.db.DBTelemetryClusterInfo.FIELD_VERSION;

class TelemetryResponseFactory {
    private static final String CURRENT_USER = "current_user";
    private static final String USER_TELEMETRY_SETTINGS = "user_telemetry_settings";
    private static final String CLUSTER = "cluster";
    private static final String LICENSE = "license";
    private static final String PLUGIN = "plugin";
    private static final String SEARCH_CLUSTER = "search_cluster";
    private final ObjectMapper objectMapper;

    @Inject
    public TelemetryResponseFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    private static boolean isLeader(Map<String, Object> n) {
        if (n.get(FIELD_IS_LEADER) instanceof Boolean isLeader) {
            return isLeader;
        }
        return false;
    }

    Map<String, Object> createTelemetryResponse(Map<String, Object> clusterInfo,
                                                Map<String, Object> userInfo,
                                                Map<String, Object> pluginInfo,
                                                Map<String, Object> searchClusterInfo,
                                                List<TelemetryLicenseStatus> licenseStatuses,
                                                TelemetryUserSettings telemetryUserSettings) {
        Map<String, Object> telemetryResponse = new LinkedHashMap<>();
        telemetryResponse.put(CURRENT_USER, userInfo);
        telemetryResponse.put(USER_TELEMETRY_SETTINGS, telemetryUserSettings);
        telemetryResponse.put(CLUSTER, clusterInfo);
        telemetryResponse.put(LICENSE, createLicenseInfo(licenseStatuses));
        telemetryResponse.put(PLUGIN, pluginInfo);
        telemetryResponse.put(SEARCH_CLUSTER, searchClusterInfo);
        return telemetryResponse;
    }

    Map<String, Object> createTelemetryDisabledResponse(TelemetryUserSettings telemetryUserSettings) {
        return Map.of(USER_TELEMETRY_SETTINGS, telemetryUserSettings);
    }

    Map<String, Object> createUserInfo(String userHash,
                                       boolean isLocalAdmin,
                                       int rolesCount,
                                       int teamsCount) {
        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("user", userHash);
        userInfo.putAll(flatten(Map.of(CURRENT_USER, Map.of(
                "is_local_admin", isLocalAdmin,
                "roles_count", rolesCount,
                "teams_count", teamsCount

        ))));
        return userInfo;
    }

    Map<String, Object> createClusterInfo(String clusterId,
                                          DateTime clusterCreationDate,
                                          Map<String, Map<String, Object>> nodes,
                                          long averageLastMonthTraffic,
                                          long usersCount,
                                          int licenseCount) {
        Map<String, Object> clusterInfo = new LinkedHashMap<>();
        clusterInfo.put("cluster_id", clusterId);
        clusterInfo.put("cluster_creation_date", clusterCreationDate);
        clusterInfo.put("nodes_count", nodes.size());
        clusterInfo.put("average_last_month_traffic", averageLastMonthTraffic);
        clusterInfo.put("users_count", usersCount);
        clusterInfo.put("license_count", licenseCount);
        clusterInfo.put("node_leader.app_version", leaderNodeVersion(nodes));
        clusterInfo.put("nodes", nodes);
        return clusterInfo;

    }

    private Object leaderNodeVersion(Map<String, Map<String, Object>> nodes) {
        return nodes.values().stream()
                .filter(TelemetryResponseFactory::isLeader)
                .map(stringObjectMap -> stringObjectMap.get(FIELD_VERSION))
                .findFirst()
                .orElse("unknown");
    }

    Map<String, Object> createPluginInfo(boolean isEnterprisePluginInstalled,
                                         List<String> plugins) {
        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put(f("%s.is_enterprise_plugin_installed", PLUGIN), isEnterprisePluginInstalled);
        userInfo.putAll(flatten(Map.of(PLUGIN, Map.of("installed_plugins", plugins))));
        return userInfo;
    }

    Map<String, Object> createSearchClusterInfo(int nodesCount,
                                                String version,
                                                Map<String, NodeInfo> nodes) {
        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put(f("%s.nodes_count", SEARCH_CLUSTER), nodesCount);
        userInfo.put(f("%s.version", SEARCH_CLUSTER), version);
        userInfo.putAll(flatten(Map.of(SEARCH_CLUSTER, Map.of("nodes", nodes))));
        return userInfo;
    }

    private Map<String, Object> createLicenseInfo(List<TelemetryLicenseStatus> telemetryLicenseStatuses) {
        Map<String, List<TelemetryLicenseStatus>> groupedBySubject = telemetryLicenseStatuses.stream()
                .collect(Collectors.groupingBy(TelemetryLicenseStatus::subject));

        List<TelemetryLicenseStatus> uniqueLicenses = groupedBySubject.values().stream()
                .map(this::filter).flatMap(Collection::stream).toList();
        Map<String, Object> licenses = new LinkedHashMap<>();
        for (TelemetryLicenseStatus l : uniqueLicenses) {
            String licenseString = formatLicenseString(l);
            licenses.put(f("%s.expired", licenseString), l.expired());
            licenses.put(f("%s.valid", licenseString), l.valid());
            licenses.put(f("%s.violated", licenseString), l.violated());
            licenses.put(f("%s.expiration_date", licenseString), l.expirationDate());
        }
        return licenses;
    }

    private List<TelemetryLicenseStatus> filter(List<TelemetryLicenseStatus> telemetryLicenseStatuses1) {
        Comparator<TelemetryLicenseStatus> compareIsValid = Comparator.comparing(TelemetryLicenseStatus::valid).reversed();
        Comparator<TelemetryLicenseStatus> compareIsExpired = Comparator.comparing(TelemetryLicenseStatus::expired);
        Comparator<TelemetryLicenseStatus> compareByDate = Comparator.comparing(TelemetryLicenseStatus::expirationDate).reversed();

        return telemetryLicenseStatuses1.stream()
                .min(compareIsValid.thenComparing(compareIsExpired).thenComparing(compareByDate))
                .map(List::of)
                .orElse(List.of());
    }

    private String formatLicenseString(TelemetryLicenseStatus telemetryLicenseStatus) {
        return telemetryLicenseStatus.subject().replace("/", ".").substring(1);
    }

    private Map<String, Object> flatten(Object o) {
        try {
            return JsonFlattener.flattenAsMap(objectMapper.writeValueAsString(o));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(f("Couldn't serialize %s!", o), e);
        }
    }
}
