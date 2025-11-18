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
package org.graylog2.telemetry.cluster.db;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog2.database.MongoEntity;
import org.joda.time.DateTime;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = TelemetryClusterInfoDto.Builder.class)
public abstract class TelemetryClusterInfoDto implements MongoEntity {
    public static final String FIELD_ID = "_id";
    public static final String FIELD_NODE_ID = "node_id";
    public static final String FIELD_CLUSTER_ID = "cluster_id";
    public static final String FIELD_CODENAME = "codename";
    public static final String FIELD_FACILITY = "facility";
    public static final String FIELD_HOSTNAME = "hostname";
    public static final String FIELD_IS_LEADER = "is_leader";
    public static final String FIELD_IS_PROCESSING = "is_processing";
    public static final String FIELD_LB_STATUS = "lb_status";
    public static final String FIELD_LIFECYCLE = "lifecycle";
    public static final String FIELD_OPERATING_SYSTEM = "operating_system";
    public static final String FIELD_STARTED_AT = "started_at";
    public static final String FIELD_TIMEZONE = "timezone";
    public static final String FIELD_UPDATED_AT = "updated_at";
    public static final String FIELD_VERSION = "version";
    public static final String FIELD_JVM_HEAP_USED = "jvm_heap_used";
    public static final String FIELD_JVM_HEAP_COMMITTED = "jvm_heap_committed";
    public static final String FIELD_JVM_HEAP_MAX = "jvm_heap_max";
    public static final String FIELD_MEMORY_TOTAL = "memory_total";
    public static final String FIELD_CPU_CORES = "cpu_cores";

    @Id
    @ObjectId
    @Nullable
    @JsonIgnore
    public abstract String id();

    @JsonProperty(FIELD_NODE_ID)
    public abstract String nodeId();

    @JsonProperty(FIELD_CLUSTER_ID)
    public abstract String clusterId();

    @JsonProperty(FIELD_CODENAME)
    public abstract String codename();

    @JsonProperty(FIELD_FACILITY)
    public abstract String facility();

    @JsonProperty(FIELD_HOSTNAME)
    public abstract String hostname();

    @JsonProperty(FIELD_IS_LEADER)
    public abstract Boolean isLeader();

    @JsonProperty(FIELD_IS_PROCESSING)
    public abstract Boolean isProcessing();

    @JsonProperty(FIELD_LB_STATUS)
    public abstract String lbStatus();

    @JsonProperty(FIELD_LIFECYCLE)
    public abstract String lifecycle();

    @JsonProperty(FIELD_OPERATING_SYSTEM)
    public abstract String operatingSystem();

    @JsonProperty(FIELD_STARTED_AT)
    public abstract DateTime startedAt();

    @JsonProperty(FIELD_TIMEZONE)
    public abstract String timezone();

    @JsonProperty(FIELD_JVM_HEAP_USED)
    @Nullable
    public abstract Long jvmHeapUsed();

    @JsonProperty(FIELD_JVM_HEAP_COMMITTED)
    @Nullable
    public abstract Long jvmHeapCommitted();

    @JsonProperty(FIELD_JVM_HEAP_MAX)
    @Nullable
    public abstract Long jvmHeapMax();

    @JsonProperty(FIELD_MEMORY_TOTAL)
    @Nullable
    public abstract Long memoryTotal();

    @JsonProperty(FIELD_CPU_CORES)
    @Nullable
    public abstract Integer cpuCores();

    @JsonIgnore
    @Nullable
    public abstract DateTime updatedAt();

    @JsonProperty(FIELD_VERSION)
    public abstract String version();

    public abstract Builder toBuilder();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_TelemetryClusterInfoDto.Builder();
        }

        @Id
        @ObjectId
        @JsonProperty(FIELD_ID)
        public abstract Builder id(String id);

        @JsonProperty(FIELD_NODE_ID)
        public abstract Builder nodeId(String nodeId);

        @JsonProperty(FIELD_CLUSTER_ID)
        public abstract Builder clusterId(String clusterId);

        @JsonProperty(FIELD_CODENAME)
        public abstract Builder codename(String codename);

        @JsonProperty(FIELD_FACILITY)
        public abstract Builder facility(String facility);

        @JsonProperty(FIELD_HOSTNAME)
        public abstract Builder hostname(String hostname);

        @JsonProperty(FIELD_IS_LEADER)
        public abstract Builder isLeader(Boolean isLeader);

        @JsonProperty(FIELD_IS_PROCESSING)
        public abstract Builder isProcessing(Boolean isProcessing);

        @JsonProperty(FIELD_LB_STATUS)
        public abstract Builder lbStatus(String lbStatus);

        @JsonProperty(FIELD_LIFECYCLE)
        public abstract Builder lifecycle(String lifecycle);

        @JsonProperty(FIELD_OPERATING_SYSTEM)
        public abstract Builder operatingSystem(String operatingSystem);

        @JsonProperty(FIELD_STARTED_AT)
        public abstract Builder startedAt(DateTime startedAt);

        @JsonProperty(FIELD_TIMEZONE)
        public abstract Builder timezone(String timezone);

        @JsonProperty(FIELD_UPDATED_AT)
        public abstract Builder updatedAt(DateTime updatedAt);

        @JsonProperty(FIELD_VERSION)
        public abstract Builder version(String version);

        @JsonProperty(FIELD_JVM_HEAP_USED)
        public abstract Builder jvmHeapUsed(Long jvmHeapUsed);

        @JsonProperty(FIELD_JVM_HEAP_COMMITTED)
        public abstract Builder jvmHeapCommitted(Long jvmHeapCommitted);

        @JsonProperty(FIELD_JVM_HEAP_MAX)
        public abstract Builder jvmHeapMax(Long jvmHeapMax);

        @JsonProperty(FIELD_MEMORY_TOTAL)
        public abstract Builder memoryTotal(Long memoryTotal);

        @JsonProperty(FIELD_CPU_CORES)
        public abstract Builder cpuCores(Integer cpuCores);

        public abstract TelemetryClusterInfoDto build();
    }
}
