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

    @Parameter(value = "opensearch_data_location")
    private String opensearchDataLocation = "./data-node/bin/data";

    @Parameter(value = "opensearch_logs_location")
    private String opensearchLogsLocation = "./data-node/bin/logs";

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

    public String getOpensearchDataLocation() {
        return opensearchDataLocation;
    }

    public String getOpensearchLogsLocation() {
        return opensearchLogsLocation;
    }

    public Integer getProcessLogsBufferSize() {
        return logs;
    }
}
