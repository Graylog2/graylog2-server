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
                    path, mount, dev, total, free, available, used, usedPercent,
                    inodesTotal, inodesFree, inodesUsed, inodesUsedPercent,
                    diskReads, diskWrites, diskReadBytes, diskWriteBytes, diskQueue, diskServiceTime);
        }

        public static Filesystem create(String path,
                                        long total,
                                        long free,
                                        long available,
                                        long used,
                                        short usedPercent) {
            return create(path, null, null, total, free, available, used, usedPercent,
                    -1L, -1L, -1L, (short) -1, -1L, -1L, -1L, -1L, -1L, -1L);
        }
    }
}
