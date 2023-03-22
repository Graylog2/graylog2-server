package org.graylog2.telemetry.license;

import java.util.List;

public class DefaultTelemetryLicenseStatusProvider implements TelemetryLicenseStatusProvider {
    @Override
    public List<TelemetryLicenseStatus> status() {
        return List.of();
    }
}
