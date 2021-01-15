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
import org.graylog.plugins.sidecar.rest.requests.ConfigurationAssignment;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.util.List;

@AutoValue
@JsonAutoDetect
public abstract class SidecarSummary {
    @JsonProperty("node_id")
    public abstract String nodeId();

    @JsonProperty("node_name")
    public abstract String nodeName();

    @JsonProperty("node_details")
    public abstract NodeDetails nodeDetails();

    @JsonProperty("assignments")
    public abstract List<ConfigurationAssignment> assignments();

    @JsonProperty("last_seen")
    public abstract DateTime lastSeen();

    @JsonProperty("sidecar_version")
    public abstract String sidecarVersion();

    @Nullable
    @JsonProperty("collectors")
    public abstract List<String> collectors();

    @JsonProperty
    public abstract boolean active();

    public static Builder builder() {
        return new AutoValue_SidecarSummary.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder nodeId(String nodeId);
        public abstract Builder nodeName(String nodeName);
        public abstract Builder nodeDetails(NodeDetails nodeDetails);
        public abstract Builder assignments(List<ConfigurationAssignment> assignments);
        public abstract Builder lastSeen(DateTime lastSeen);
        public abstract Builder sidecarVersion(String sidecarVersion);
        public abstract Builder active(boolean active);
        public abstract Builder collectors(List<String> collectors);
        public abstract SidecarSummary build();
    }

    @JsonCreator
    public static SidecarSummary create(@JsonProperty("node_id") String nodeId,
                                        @JsonProperty("node_name") String nodeName,
                                        @JsonProperty("node_details") NodeDetails nodeDetails,
                                        @JsonProperty("assignments") List<ConfigurationAssignment> assignments,
                                        @JsonProperty("last_seen") DateTime lastSeen,
                                        @JsonProperty("sidecar_version") String sidecarVersion,
                                        @JsonProperty("active") boolean active,
                                        @JsonProperty("collectors") @Nullable List<String> collectors) {
        return builder()
                .nodeId(nodeId)
                .nodeName(nodeName)
                .nodeDetails(nodeDetails)
                .assignments(assignments)
                .lastSeen(lastSeen)
                .sidecarVersion(sidecarVersion)
                .active(active)
                .collectors(collectors)
                .build();

    }
}
