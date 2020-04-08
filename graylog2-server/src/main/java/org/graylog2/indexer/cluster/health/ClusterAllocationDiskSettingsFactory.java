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

import org.elasticsearch.common.unit.ByteSizeValue;

import java.util.stream.Stream;

public class ClusterAllocationDiskSettingsFactory {

    public static ClusterAllocationDiskSettings create(boolean enabled, String low, String high, String floodStage) throws Exception {
        if (!enabled) {
            return ClusterAllocationDiskSettings.create(enabled, null);
        }
        return ClusterAllocationDiskSettings.create(enabled, createWatermarkSettings(low, high, floodStage));
    }

    private static WatermarkSettings.SettingsType getType(String value) {
        if (value.trim().endsWith("%")) {
            return WatermarkSettings.SettingsType.PERCENTAGE;
        }
        return WatermarkSettings.SettingsType.ABSOLUTE;
    }

    private static WatermarkSettings<?> createWatermarkSettings(String low, String high, String floodStage) throws Exception {
        WatermarkSettings.SettingsType lowType = getType(low);
        WatermarkSettings.SettingsType highType = getType(high);
        WatermarkSettings.SettingsType floodStageType = getType(floodStage);
        if (Stream.of(lowType, highType, floodStageType).allMatch(s -> s == WatermarkSettings.SettingsType.ABSOLUTE)) {
            return new AbsoluteValueWatermarkSettings(
                    ByteSizeValue.parseBytesSizeValue(low, "lowWatermark"),
                    ByteSizeValue.parseBytesSizeValue(high, "highWatermark"),
                    ByteSizeValue.parseBytesSizeValue(floodStage, "floodStageWatermark")
            );
        } else if (Stream.of(lowType, highType, floodStageType).allMatch(s -> s == WatermarkSettings.SettingsType.PERCENTAGE)) {
            return new PercentageWatermarkSettings(
                    getPercentageValue(low),
                    getPercentageValue(high),
                    getPercentageValue(floodStage)
            );
        }
        throw new Exception("Error creating ClusterAllocationDiskWatermarkSettings. This should never happen.");
    }

    private static Double getPercentageValue(String value) {
        return Double.parseDouble(value.trim().replace("%", ""));
    }
}
