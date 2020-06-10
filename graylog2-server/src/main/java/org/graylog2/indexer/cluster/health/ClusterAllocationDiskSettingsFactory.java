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

import java.util.stream.Stream;

public class ClusterAllocationDiskSettingsFactory {

    public static ClusterAllocationDiskSettings create(boolean enabled, String low, String high, String floodStage) {
        if (!enabled) {
            return ClusterAllocationDiskSettings.create(enabled, null);
        }
        return ClusterAllocationDiskSettings.create(enabled, createWatermarkSettings(low, high, floodStage));
    }

    private static WatermarkSettings<?> createWatermarkSettings(String low, String high, String floodStage) {
        WatermarkSettings.SettingsType lowType = getType(low);
        WatermarkSettings.SettingsType highType = getType(high);
        if (Stream.of(lowType, highType).allMatch(s -> s == WatermarkSettings.SettingsType.ABSOLUTE)) {
            AbsoluteValueWatermarkSettings.Builder builder = new AbsoluteValueWatermarkSettings.Builder()
                .low(SIUnitParser.parseBytesSizeValue(low))
                .high(SIUnitParser.parseBytesSizeValue(high));
            if (!floodStage.isEmpty()) {
                builder.floodStage(SIUnitParser.parseBytesSizeValue(floodStage));
            }
            return builder.build();
        } else if (Stream.of(lowType, highType).allMatch(s -> s == WatermarkSettings.SettingsType.PERCENTAGE)) {
            PercentageWatermarkSettings.Builder builder = new PercentageWatermarkSettings.Builder()
                .low(getPercentageValue(low))
                .high(getPercentageValue(high));
            if (!floodStage.isEmpty()) {
                builder.floodStage(getPercentageValue(floodStage));
            }
            return builder.build();
        }
        throw new IllegalStateException("Error creating ClusterAllocationDiskWatermarkSettings. This should never happen.");
    }

    private static WatermarkSettings.SettingsType getType(String value) {
        if (value.trim().endsWith("%")) {
            return WatermarkSettings.SettingsType.PERCENTAGE;
        }
        return WatermarkSettings.SettingsType.ABSOLUTE;
    }

    private static Double getPercentageValue(String value) {
        return Double.parseDouble(value.trim().replace("%", ""));
    }
}
