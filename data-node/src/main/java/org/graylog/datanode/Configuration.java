package org.graylog.datanode;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import com.github.joschi.jadconfig.validators.StringNotBlankValidator;

public class Configuration {
    @Parameter(value = "installation_source", validator = StringNotBlankValidator.class)
    private String installationSource = "unknown";

    @Parameter(value = "skip_preflight_checks")
    private boolean skipPreflightChecks = false;

    @Parameter(value = "shutdown_timeout", validator = PositiveIntegerValidator.class)
    protected int shutdownTimeout = 30000;

    @Parameter(value = "is_leader")
    private boolean isLeader = true;

    @Parameter("disable_native_system_stats_collector")
    private boolean disableNativeSystemStatsCollector = false;

    @Parameter(value = "opensearch_location")
    private String opensearchLocation = "./data-node/bin/opensearch-2.4.1";

    @Parameter(value = "opensearch_version")
    private String opensearchVersion = "2.4.1";

    @Parameter(value = "process_logs_buffer_size")
    private Integer logs = 500;

    public String getInstallationSource() {
        return installationSource;
    }

    public boolean getSkipPreflightChecks() {
        return skipPreflightChecks;
    }

    public int getShutdownTimeout() {
        return shutdownTimeout;
    }

    public boolean isDisableNativeSystemStatsCollector() {
        return disableNativeSystemStatsCollector;
    }

    public boolean isLeader() {
        return isLeader;
    }

    public String getOpensearchLocation() {
        return opensearchLocation;
    }

    public String getOpensearchVersion() {
        return opensearchVersion;
    }

    public Integer getProcessLogsBufferSize() {
        return logs;
    }
}
