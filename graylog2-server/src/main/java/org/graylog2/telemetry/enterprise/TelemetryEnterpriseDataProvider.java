package org.graylog2.telemetry.enterprise;

import java.util.List;

public interface TelemetryEnterpriseDataProvider {

    List<TelemetryLicenseStatus> licenseStatus();

    int teamsCount(String userId);

}
