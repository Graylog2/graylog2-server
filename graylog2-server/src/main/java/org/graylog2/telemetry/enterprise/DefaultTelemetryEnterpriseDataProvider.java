package org.graylog2.telemetry.enterprise;

import java.util.List;

public class DefaultTelemetryEnterpriseDataProvider implements TelemetryEnterpriseDataProvider {
    @Override
    public List<TelemetryLicenseStatus> licenseStatus() {
        return List.of();
    }

    @Override
    public int teamsCount(String userId) {
        return 0;
    }
}
