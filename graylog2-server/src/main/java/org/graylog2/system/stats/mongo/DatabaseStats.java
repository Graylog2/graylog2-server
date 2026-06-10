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
package org.graylog2.system.stats.mongo;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

/**
 * @see <a href="http://docs.mongodb.org/manual/reference/command/dbStats/">Diagnostic Commands &gt; dbStats</a>
 */
@JsonAutoDetect
@AutoValue
public abstract class DatabaseStats {
    @JsonProperty("db")
    public abstract String db();

    @JsonProperty("collections")
    public abstract long collections();

    @JsonProperty("objects")
    public abstract long objects();

    @JsonProperty("avg_obj_size")
    public abstract double avgObjSize();

    @JsonProperty("data_size")
    public abstract long dataSize();

    @JsonProperty("storage_size")
    public abstract long storageSize();

    @JsonProperty("num_extents")
    @Nullable
    public abstract Long numExtents();

    @JsonProperty("indexes")
    public abstract long indexes();

    @JsonProperty("index_size")
    public abstract long indexSize();

    @JsonProperty("file_size")
    @Nullable
    public abstract Long fileSize();

    @JsonProperty("ns_size_mb")
    @Nullable
    public abstract Long nsSizeMB();

    @JsonProperty("extent_free_list")
    @Nullable
    public abstract ExtentFreeList extentFreeList();

    @JsonProperty("data_file_version")
    @Nullable
    public abstract DataFileVersion dataFileVersion();

    public static DatabaseStats create(String db,
                                       long collections,
                                       long objects,
                                       double avgObjSize,
                                       long dataSize,
                                       long storageSize,
                                       @Nullable Long numExtents,
                                       long indexes,
                                       long indexSize,
                                       @Nullable Long fileSize,
                                       @Nullable Long nsSizeMB,
                                       @Nullable ExtentFreeList extentFreeList,
                                       @Nullable DataFileVersion dataFileVersion) {
        return new AutoValue_DatabaseStats(db, collections, objects, avgObjSize, dataSize, storageSize, numExtents,
                indexes, indexSize, fileSize, nsSizeMB, extentFreeList, dataFileVersion);
    }

    @JsonAutoDetect
    @AutoValue
    public abstract static class ExtentFreeList {
        @JsonProperty("num")
        public abstract int num();

        @JsonProperty("total_size")
        public abstract int totalSize();

        public static ExtentFreeList create(int num,
                                            int totalSize) {
            return new AutoValue_DatabaseStats_ExtentFreeList(num, totalSize);
        }
    }

    @JsonAutoDetect
    @AutoValue
    public abstract static class DataFileVersion {
        @JsonProperty("major")
        public abstract int major();

        @JsonProperty("minor")
        public abstract int minor();

        public static DataFileVersion create(int major,
                                             int minor) {
            return new AutoValue_DatabaseStats_DataFileVersion(major, minor);
        }
    }
}
