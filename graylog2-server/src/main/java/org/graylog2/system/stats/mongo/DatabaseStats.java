/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.system.stats.mongo;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;

/**
 * @see <a href="http://docs.mongodb.org/manual/reference/command/dbStats/">Diagnostic Commands &gt; dbStats</a>
 */
@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class DatabaseStats {
    @JsonProperty
    public abstract String db();

    @JsonProperty
    public abstract long collections();

    @JsonProperty
    public abstract long objects();

    @JsonProperty
    public abstract double avgObjSize();

    @JsonProperty
    public abstract long dataSize();

    @JsonProperty
    public abstract long storageSize();

    @JsonProperty
    public abstract long numExtents();

    @JsonProperty
    public abstract long indexes();

    @JsonProperty
    public abstract long indexSize();

    @JsonProperty
    @Nullable
    public abstract Long fileSize();

    @JsonProperty
    @Nullable
    public abstract Long nsSizeMB();

    @JsonProperty
    @Nullable
    public abstract ExtentFreeList extentFreeList();

    @JsonProperty
    @Nullable
    public abstract DataFileVersion dataFileVersion();

    public static DatabaseStats create(String db,
                                       long collections,
                                       long objects,
                                       double avgObjSize,
                                       long dataSize,
                                       long storageSize,
                                       long numExtents,
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
@WithBeanGetter
    public abstract static class ExtentFreeList {
        @JsonProperty
        public abstract int num();

        @JsonProperty
        public abstract int totalSize();

        public static ExtentFreeList create(int num,
                                            int totalSize) {
            return new AutoValue_DatabaseStats_ExtentFreeList(num, totalSize);
        }
    }

    @JsonAutoDetect
    @AutoValue
@WithBeanGetter
    public abstract static class DataFileVersion {
        @JsonProperty
        public abstract int major();

        @JsonProperty
        public abstract int minor();

        public static DataFileVersion create(int major,
                                             int minor) {
            return new AutoValue_DatabaseStats_DataFileVersion(major, minor);
        }
    }
}
