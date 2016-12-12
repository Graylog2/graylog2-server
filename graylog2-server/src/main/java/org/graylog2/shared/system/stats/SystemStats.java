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
package org.graylog2.shared.system.stats;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.shared.system.stats.fs.FsStats;
import org.graylog2.shared.system.stats.jvm.JvmStats;
import org.graylog2.shared.system.stats.network.NetworkStats;
import org.graylog2.shared.system.stats.os.OsStats;
import org.graylog2.shared.system.stats.process.ProcessStats;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class SystemStats {
    @JsonProperty("fs")
    public abstract FsStats fsStats();

    @JsonProperty("jvm")
    public abstract JvmStats jvmStats();

    @JsonProperty("network")
    public abstract NetworkStats networkStats();

    @JsonProperty("os")
    public abstract OsStats osStats();

    @JsonProperty("process")
    public abstract ProcessStats processStats();

    public static SystemStats create(FsStats fsStats,
                                     JvmStats jvmStats,
                                     NetworkStats networkStats,
                                     OsStats osStats,
                                     ProcessStats processStats) {
        return new AutoValue_SystemStats(fsStats, jvmStats, networkStats, osStats, processStats);
    }
}
