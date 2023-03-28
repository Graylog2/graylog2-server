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
