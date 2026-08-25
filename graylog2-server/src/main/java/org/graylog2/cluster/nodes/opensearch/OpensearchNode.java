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
package org.graylog2.cluster.nodes.opensearch;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.utilities.lucene.InMemorySearchableEntity;
import org.graylog2.utilities.lucene.LuceneDocBuilder;

import java.util.List;

public record OpensearchNode(
        @JsonProperty(FIELD_ID) String id,
        @JsonProperty(FIELD_NAME) String name,
        @JsonProperty(FIELD_VERSION) String version,
        @JsonProperty(FIELD_ROLES) List<String> roles,
        @JsonProperty(FIELD_JVM_HEAP_MAX) Long jvmHeapMaxBytes,
        @JsonProperty(FIELD_JVM_HEAP_USED_PERCENT) Double jvmHeapUsedPercent,
        @JsonProperty(FIELD_CPU_USED_PERCENT) Double cpuUsedPercent,
        @JsonProperty(FIELD_DISK_USED_PERCENT) Double diskUsedPercent,
        @JsonProperty(FIELD_DISK_USED) Long diskUsedBytes,
        @JsonProperty(FIELD_DISK_TOTAL) Long diskTotalBytes
) implements InMemorySearchableEntity {

    public static final String FIELD_ID = "id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_VERSION = "version";
    public static final String FIELD_ROLES = "roles";
    public static final String FIELD_JVM_HEAP_MAX = "jvm_heap_max";
    public static final String FIELD_JVM_HEAP_USED_PERCENT = "jvm_heap_used_percent";
    public static final String FIELD_CPU_USED_PERCENT = "cpu_used_percent";
    public static final String FIELD_DISK_USED_PERCENT = "disk_used_percent";
    public static final String FIELD_DISK_USED = "disk_used";
    public static final String FIELD_DISK_TOTAL = "disk_total";

    @JsonIgnore
    @Override
    public void buildLuceneDoc(LuceneDocBuilder builder) {
        builder.stringVal(FIELD_ID, id);
        builder.stringVal(FIELD_NAME, name);
        builder.stringVal(FIELD_VERSION, version);
        builder.stringVal(FIELD_ROLES, roles == null ? null : String.join(",", roles));
        builder.longVal(FIELD_JVM_HEAP_MAX, jvmHeapMaxBytes);
        builder.doubleVal(FIELD_JVM_HEAP_USED_PERCENT, jvmHeapUsedPercent);
        builder.doubleVal(FIELD_CPU_USED_PERCENT, cpuUsedPercent);
        builder.doubleVal(FIELD_DISK_USED_PERCENT, diskUsedPercent);
        builder.longVal(FIELD_DISK_USED, diskUsedBytes);
        builder.longVal(FIELD_DISK_TOTAL, diskTotalBytes);
    }
}
