package org.graylog2.telemetry.license;

import java.util.List;

public interface TelemetryLicenseStatusProvider {

    List<TelemetryLicenseStatus> status();

}
