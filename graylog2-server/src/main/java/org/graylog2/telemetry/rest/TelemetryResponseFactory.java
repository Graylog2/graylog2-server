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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import org.graylog2.system.stats.elasticsearch.NodeInfo;
import org.graylog2.system.traffic.TrafficCounterService.TrafficHistogram;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;

import static org.graylog2.shared.utilities.StringUtils.f;
import static org.graylog2.telemetry.cluster.db.DBTelemetryClusterInfo.FIELD_IS_LEADER;
import static org.graylog2.telemetry.cluster.db.DBTelemetryClusterInfo.FIELD_VERSION;

public class TelemetryResponseFactory {
    private static final String PREFIX_USER = "user";
    private static final String USER_TELEMETRY_SETTINGS = "user_telemetry_settings";
    private static final String PLUGIN = "plugin";
    private static final String SEARCH_CLUSTER = "search_cluster";
    private static final String DATA_NODES = "data_nodes";

    public static final String CLUSTER = "cluster";
    public static final String LICENSE = "license";
    public static final String LICENSE_COUNT = "license_count";
    public static final String CURRENT_USER = "current_user";
    public static final String TEAMS_COUNT = "%s_team_count".formatted(PREFIX_USER);

    private final ObjectMapper mapper;

    @Inject
    public TelemetryResponseFactory(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    private static boolean isLeader(Map<String, Object> n) {
        if (n.get(FIELD_IS_LEADER) instanceof Boolean isLeader) {
            return isLeader;
        }
        return false;
    }

    ObjectNode createTelemetryResponse(ObjectNode clusterInfo,
                                       ObjectNode userInfo,
                                       ObjectNode pluginInfo,
                                       ObjectNode searchClusterInfo,
                                       TelemetryUserSettings telemetryUserSettings,
                                       Map<String, Object> dataNodeInfo) {
        ObjectNode telemetryResponse = mapper.createObjectNode();
        telemetryResponse.set(CURRENT_USER, userInfo);
        telemetryResponse.set(USER_TELEMETRY_SETTINGS, mapper.convertValue(telemetryUserSettings, JsonNode.class));
        telemetryResponse.set(CLUSTER, clusterInfo);
        telemetryResponse.set(LICENSE, mapper.createObjectNode());
        telemetryResponse.set(PLUGIN, pluginInfo);
        telemetryResponse.set(SEARCH_CLUSTER, searchClusterInfo);
        telemetryResponse.set(DATA_NODES, mapper.convertValue(dataNodeInfo, JsonNode.class));
        return telemetryResponse;
    }

    ObjectNode createTelemetryDisabledResponse(TelemetryUserSettings telemetryUserSettings) {
        ObjectNode settings = mapper.createObjectNode();
        settings.set(USER_TELEMETRY_SETTINGS, mapper.convertValue(telemetryUserSettings, JsonNode.class));
        return settings;
    }

    ObjectNode createUserInfo(String userHash,
                              boolean isLocalAdmin,
                              int rolesCount) {
        ObjectNode userInfo = mapper.createObjectNode();
        userInfo.put("user", userHash);
        userInfo.put(f("%s_is_local_admin", PREFIX_USER), isLocalAdmin);
        userInfo.put(f("%s_role_count", PREFIX_USER), rolesCount);
        userInfo.put(TEAMS_COUNT, 0);
        return userInfo;
    }

    ObjectNode createClusterInfo(String clusterId,
                                 DateTime clusterCreationDate,
                                 Map<String, Map<String, Object>> nodes,
                                 TrafficHistogram trafficLastMonth,
                                 long usersCount,
                                 String installationSource
    ) {
        ObjectNode clusterInfo = mapper.createObjectNode();
        clusterInfo.put("cluster_id", clusterId);
        clusterInfo.set("cluster_creation_date", mapper.convertValue(clusterCreationDate, JsonNode.class));
        clusterInfo.put("nodes_count", nodes.size());
        clusterInfo.put("traffic_last_month", sumTraffic(trafficLastMonth.output()));
        clusterInfo.put("input_traffic_last_month", sumTraffic(trafficLastMonth.input()));
        clusterInfo.put("users_count", usersCount);
        clusterInfo.put(LICENSE_COUNT, 0);
        clusterInfo.set("node_leader_app_version", mapper.convertValue(leaderNodeVersion(nodes), JsonNode.class));
        clusterInfo.put("installation_source", installationSource);
        clusterInfo.set("nodes", mapper.convertValue(nodes, JsonNode.class));
        return clusterInfo;

    }

    private static long sumTraffic(Map<DateTime, Long> traffic) {
        return traffic.values().stream().mapToLong(Long::longValue).sum();
    }

    private Object leaderNodeVersion(Map<String, Map<String, Object>> nodes) {
        return nodes.values().stream()
                .filter(TelemetryResponseFactory::isLeader)
                .map(stringObjectMap -> stringObjectMap.get(FIELD_VERSION))
                .findFirst()
                .orElse("unknown");
    }

    ObjectNode createPluginInfo(boolean isEnterprisePluginInstalled,
                                List<String> plugins) {
        ObjectNode userInfo = mapper.createObjectNode();
        userInfo.put(f("%s_is_enterprise_plugin_installed", PLUGIN), isEnterprisePluginInstalled);
        userInfo.set("plugins", mapper.convertValue(plugins, JsonNode.class));
        return userInfo;
    }

    ObjectNode createSearchClusterInfo(int nodesCount,
                                       String version,
                                       Map<String, NodeInfo> nodes) {
        ObjectNode userInfo = mapper.createObjectNode();
        userInfo.put(f("%s_nodes_count", SEARCH_CLUSTER), nodesCount);
        userInfo.put(f("%s_version", SEARCH_CLUSTER), version);
        userInfo.set("nodes", mapper.convertValue(nodes, JsonNode.class));
        return userInfo;
    }
}
