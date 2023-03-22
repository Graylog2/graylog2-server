package org.graylog2.telemetry.rest;

import org.graylog2.rest.models.system.responses.SystemOverviewResponse;
import org.graylog2.telemetry.license.TelemetryLicenseStatus;

import java.util.List;
import java.util.Map;

public record TelemetryResponse(ClusterInfo clusterInfo, LicenseInfo licenseInfo) {


    public record ClusterInfo(String clusterId,
                              Map<String, SystemOverviewResponse> nodes,
                              long averageLastMonthTraffic) {

    }

    public record LicenseInfo(List<TelemetryLicenseStatus> licenses) {

    }

}
