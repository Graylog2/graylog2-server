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
package org.graylog2.indexer.cluster.health;

import com.google.auto.value.AutoValue;
import org.elasticsearch.common.unit.ByteSizeValue;

import javax.annotation.Nullable;

@AutoValue
public abstract class NodeDiskUsageStats {
    public static final double DEFAULT_DISK_USED_PERCENT = -1D;

    public abstract String name();

    public abstract String ip();

    @Nullable
    public abstract String host();

    public abstract ByteSizeValue diskTotal();

    public abstract ByteSizeValue diskUsed();

    public abstract ByteSizeValue diskAvailable();

    public abstract Double diskUsedPercent();

    public static NodeDiskUsageStats create(String name, String ip, @Nullable String host, String diskUsedString, String diskTotalString, Double diskUsedPercent) {
        ByteSizeValue diskTotal = ByteSizeValue.parseBytesSizeValue(diskTotalString, "diskTotal");
        ByteSizeValue diskUsed = ByteSizeValue.parseBytesSizeValue(diskUsedString, "diskUsed");
        ByteSizeValue diskAvailable = new ByteSizeValue(diskTotal.getBytes() - diskUsed.getBytes());
        return new AutoValue_NodeDiskUsageStats(
                name,
                ip,
                host,
                diskTotal,
                diskUsed,
                diskAvailable,
                diskUsedPercent
        );
    }
}
