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
package org.graylog2.shared.system.stats.fs;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;
import java.util.Map;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class FsStats {
    @JsonProperty
    public abstract Map<String, Filesystem> filesystems();

    public static FsStats create(Map<String, Filesystem> filesystems) {
        return new AutoValue_FsStats(filesystems);
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    public abstract static class Filesystem {
        @JsonProperty
        public abstract String path();

        @JsonProperty
        @Nullable
        public abstract String mount();

        @JsonProperty
        @Nullable
        public abstract String dev();

        @JsonProperty
        @Nullable
        public  abstract String typeName();

        @JsonProperty
        @Nullable
        public  abstract String sysTypeName();

        @JsonProperty
        public abstract long total();

        @JsonProperty
        public abstract long free();

        @JsonProperty
        public abstract long available();

        @JsonProperty
        public abstract long used();

        @JsonProperty
        public abstract short usedPercent();

        @JsonProperty
        public abstract long inodesTotal();

        @JsonProperty
        public abstract long inodesFree();

        @JsonProperty
        public abstract long inodesUsed();

        @JsonProperty
        public abstract short inodesUsedPercent();

        @JsonProperty
        public abstract long diskReads();

        @JsonProperty
        public abstract long diskWrites();

        @JsonProperty
        public abstract long diskReadBytes();

        @JsonProperty
        public abstract long diskWriteBytes();

        @JsonProperty
        public abstract double diskQueue();

        @JsonProperty
        public abstract double diskServiceTime();

        public static Filesystem create(String path,
                                        String mount,
                                        String dev,
                                        String typeName,
                                        String sysTypeName,
                                        long total,
                                        long free,
                                        long available,
                                        long used,
                                        short usedPercent,
                                        long inodesTotal,
                                        long inodesFree,
                                        long inodesUsed,
                                        short inodesUsedPercent,
                                        long diskReads,
                                        long diskWrites,
                                        long diskReadBytes,
                                        long diskWriteBytes,
                                        double diskQueue,
                                        double diskServiceTime) {
            return new AutoValue_FsStats_Filesystem(
                    path, mount, dev, typeName, sysTypeName, total, free, available, used, usedPercent,
                    inodesTotal, inodesFree, inodesUsed, inodesUsedPercent,
                    diskReads, diskWrites, diskReadBytes, diskWriteBytes, diskQueue, diskServiceTime);
        }

        public static Filesystem create(String path,
                                        long total,
                                        long free,
                                        long available,
                                        long used,
                                        short usedPercent) {
            return create(path, null, null, null, null, total, free, available, used, usedPercent,
                    -1L, -1L, -1L, (short) -1, -1L, -1L, -1L, -1L, -1L, -1L);
        }
    }
}
