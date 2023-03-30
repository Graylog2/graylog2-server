package org.graylog2.telemetry.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wnameless.json.flattener.JsonFlattener;
import org.graylog2.rest.models.system.responses.SystemOverviewResponse;
import org.graylog2.system.stats.elasticsearch.NodeInfo;
import org.graylog2.telemetry.enterprise.TelemetryLicenseStatus;
import org.joda.time.DateTime;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class TelemetryResponseFactory {
    private final ObjectMapper objectMapper;

    @Inject
    public TelemetryResponseFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> createClusterInfo(String clusterId,
                                                 DateTime clusterCreationDate,
                                                 int nodesCount,
                                                 Map<String, SystemOverviewResponse> nodes,
                                                 long averageLastMonthTraffic,
                                                 long usersCount) throws JsonProcessingException {
        Map<String, Object> clusterInfo = new LinkedHashMap<>();
        clusterInfo.put("cluster_id", clusterId);
        clusterInfo.put("cluster.cluster_creation_date", clusterCreationDate);
        clusterInfo.put("cluster.nodes_count", nodesCount);
        clusterInfo.put("cluster.average_last_month_traffic", averageLastMonthTraffic);
        clusterInfo.put("cluster.users_count", usersCount);
        clusterInfo.putAll(flatten(Map.of("cluster", nodes)));
        return clusterInfo;

    }

    public Map<String, Object> createLicenseInfo(List<TelemetryLicenseStatus> telemetryLicenseStatuses) throws JsonProcessingException {
        return flatten(Map.of("license", Map.of("license", telemetryLicenseStatuses)));
    }

    public Map<String, Object> createUserInfo(String userHash,
                                              boolean isLocalAdmin,
                                              int rolesCount,
                                              int teamsCount) throws JsonProcessingException {
        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("user", userHash);
        userInfo.putAll(flatten(Map.of("current_user", Map.of(
                "is_local_admin", isLocalAdmin,
                "roles_count", rolesCount,
                "teams_count", teamsCount

        ))));
        return userInfo;
    }

    public Map<String, Object> createPluginInfo(boolean isEnterprisePluginInstalled,
                                                List<String> plugins) throws JsonProcessingException {
        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("plugin.is_enterprise_plugin_installed", isEnterprisePluginInstalled);
        userInfo.putAll(flatten(Map.of("plugin", Map.of("installed_plugins", plugins))));
        return userInfo;
    }

    public Map<String, Object> createSearchClusterInfo(int nodesCount,
                                                       String version,
                                                       Map<String, NodeInfo> nodes) throws JsonProcessingException {
        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("search_cluster.nodes_count", nodesCount);
        userInfo.put("search_cluster.version", version);
        userInfo.putAll(flatten(Map.of("search_cluster", Map.of("nodes", nodes))));
        return userInfo;
    }

    private Map<String, Object> flatten(Object o) throws JsonProcessingException {
        return JsonFlattener.flattenAsMap(objectMapper.writeValueAsString(o));
    }
}
