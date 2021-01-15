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
import org.joda.time.DateTimeZone;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import static com.google.common.base.MoreObjects.firstNonNull;

@AutoValue
@JsonAutoDetect
public abstract class Sidecar {

    public static final String FIELD_ID = "id";
    public static final String FIELD_NODE_ID = "node_id";
    public static final String FIELD_NODE_NAME = "node_name";
    public static final String FIELD_NODE_DETAILS = "node_details";
    public static final String FIELD_ASSIGNMENTS = "assignments";
    public static final String FIELD_SIDECAR_VERSION = "sidecar_version";
    public static final String FIELD_LAST_SEEN = "last_seen";

    public static final String FIELD_OPERATING_SYSTEM = FIELD_NODE_DETAILS + ".operating_system";
    public static final String FIELD_STATUS = FIELD_NODE_DETAILS + ".status.status";

    public enum Status {
        RUNNING(0), UNKNOWN(1), FAILING(2), STOPPED(3);

        private final int statusCode;

        Status(int statusCode) {
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public static Status fromStatusCode(int statusCode) {
            switch (statusCode) {
                case 0: return RUNNING;
                case 2: return FAILING;
                case 3: return STOPPED;
                default: return UNKNOWN;
            }
        }

        public static Status fromString(String statusString) {
            return valueOf(statusString.toUpperCase(Locale.ENGLISH));
        }
    }

    @JsonProperty
    @Id
    @ObjectId
    @Nullable
    public abstract String id();

    @JsonProperty
    public abstract String nodeId();

    @JsonProperty
    public abstract String nodeName();

    @JsonProperty
    public abstract NodeDetails nodeDetails();

    @JsonProperty
    @Nullable
    public abstract List<ConfigurationAssignment> assignments();

    @JsonProperty
    public abstract String sidecarVersion();

    @JsonProperty
    public abstract DateTime lastSeen();

    public static Builder builder() {
        return new AutoValue_Sidecar.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(String id);
        public abstract Builder nodeId(String title);
        public abstract Builder nodeName(String title);
        public abstract Builder nodeDetails(NodeDetails nodeDetails);
        public abstract Builder assignments(List<ConfigurationAssignment> assignments);
        public abstract Builder sidecarVersion(String sidecarVersion);
        public abstract Builder lastSeen(DateTime lastSeen);
        public abstract Sidecar build();
    }

    @JsonCreator
    public static Sidecar create(@JsonProperty(FIELD_ID) @Id @ObjectId String id,
                                 @JsonProperty(FIELD_NODE_ID) String nodeId,
                                 @JsonProperty(FIELD_NODE_NAME) String nodeName,
                                 @JsonProperty(FIELD_NODE_DETAILS) NodeDetails nodeDetails,
                                 @JsonProperty(FIELD_ASSIGNMENTS) @Nullable List<ConfigurationAssignment> assignments,
                                 @JsonProperty(FIELD_SIDECAR_VERSION) String sidecarVersion,
                                 @JsonProperty(FIELD_LAST_SEEN) DateTime lastSeen) {

        return builder()
                .id(id)
                .nodeId(nodeId)
                .nodeName(nodeName)
                .nodeDetails(nodeDetails)
                .assignments(assignments)
                .sidecarVersion(sidecarVersion)
                .lastSeen(lastSeen)
                .build();
    }

    public static Sidecar create(@JsonProperty(FIELD_NODE_ID) String nodeId,
                                 @JsonProperty(FIELD_NODE_NAME) String nodeName,
                                 @JsonProperty(FIELD_NODE_DETAILS) NodeDetails nodeDetails,
                                 @JsonProperty(FIELD_SIDECAR_VERSION) String sidecarVersion) {

        return builder()
                .id(new org.bson.types.ObjectId().toHexString())
                .nodeId(nodeId)
                .nodeName(nodeName)
                .nodeDetails(nodeDetails)
                .sidecarVersion(sidecarVersion)
                .lastSeen(DateTime.now(DateTimeZone.UTC))
                .build();
    }

    public SidecarSummary toSummary(Predicate<Sidecar> isActiveFunction) {
        return SidecarSummary.builder()
                .nodeId(nodeId())
                .nodeName(nodeName())
                .nodeDetails(nodeDetails())
                .assignments(firstNonNull(assignments(), new ArrayList<>()))
                .lastSeen(lastSeen())
                .sidecarVersion(sidecarVersion())
                .active(isActiveFunction != null && isActiveFunction.test(this))
                .build();
    }
}
