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
package org.graylog.plugins.sidecar.rest.models;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Set;

@AutoValue
@JsonAutoDetect
public abstract class NodeDetails {
    @JsonProperty("operating_system")
    @NotNull
    @Size(min = 1)
    public abstract String operatingSystem();

    @JsonProperty("ip")
    @Nullable
    public abstract String ip();

    @JsonProperty("metrics")
    @Nullable
    public abstract NodeMetrics metrics();

    @JsonProperty("log_file_list")
    @Nullable
    public abstract List<NodeLogFile> logFileList();

    @JsonProperty("status")
    @Nullable
    public abstract CollectorStatusList statusList();

    @JsonProperty("tags")
    @Nullable
    public abstract Set<String> tags();

    @JsonProperty("collector_configuration_directory")
    @Nullable
    public abstract String collectorConfigurationDirectory();

    @JsonCreator
    public static NodeDetails create(@JsonProperty("operating_system") String operatingSystem,
                                     @JsonProperty("ip") @Nullable String ip,
                                     @JsonProperty("metrics") @Nullable NodeMetrics metrics,
                                     @JsonProperty("log_file_list") @Nullable List<NodeLogFile> logFileList,
                                     @JsonProperty("status") @Nullable CollectorStatusList statusList,
                                     @JsonProperty("tags") @Nullable Set<String> tags,
                                     @JsonProperty("collector_configuration_directory") @Nullable String configDir) {
        return new AutoValue_NodeDetails(operatingSystem, ip, metrics, logFileList, statusList, tags, configDir);
    }
}
