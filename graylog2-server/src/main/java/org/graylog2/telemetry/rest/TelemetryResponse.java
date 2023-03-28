package org.graylog2.telemetry.rest;

import org.graylog2.rest.models.system.responses.SystemOverviewResponse;
import org.graylog2.system.stats.elasticsearch.NodeInfo;
import org.graylog2.telemetry.enterprise.TelemetryLicenseStatus;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;

public record TelemetryResponse(UserInfo currentUser,
                                ClusterInfo clusterInfo,
                                LicenseInfo licenseInfo,
                                PluginInfo pluginInfo,
                                SearchClusterInfo searchClusterInfo) {


    public record UserInfo(String user,
                           boolean isLocalAdmin,
                           int rolesCount,
                           int teamsCount) {

    }

    public record ClusterInfo(String clusterId,
                              DateTime clusterCreationDate,
                              int nodesCount,
                              Map<String, SystemOverviewResponse> nodes,
                              long averageLastMonthTraffic,
                              long usersCount) {

    }

    public record LicenseInfo(List<TelemetryLicenseStatus> licenses) {

    }

    public record PluginInfo(boolean isEnterprisePluginInstalled, List<String> installedPlugins) {
    }

    public record SearchClusterInfo(int nodesCount,
                                    String version,
                                    Map<String, NodeInfo> nodes) {

    }

}
