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

import javax.annotation.Nullable;

public class PercentageWatermarkSettings implements WatermarkSettings<Double> {
    private Double low;

    private Double high;

    @Nullable
    private Double floodStage;

    private PercentageWatermarkSettings(Double low, Double high, Double floodStage) {
        this.low = low;
        this.high = high;
        this.floodStage = floodStage;
    }

    @Override
    public SettingsType type() {
        return SettingsType.PERCENTAGE;
    }

    @Override
    public Double low() {
        return low;
    }

    @Override
    public Double high() {
        return high;
    }

    @Override
    public Double floodStage() {
        return floodStage;
    }

    public static class Builder {
        private Double low;
        private Double high;
        private Double floodStage;

        public Builder() {
        }

        public PercentageWatermarkSettings.Builder low(Double low) {
            this.low = low;
            return this;
        }

        public PercentageWatermarkSettings.Builder high(Double high) {
            this.high = high;
            return this;
        }

        public PercentageWatermarkSettings.Builder floodStage(Double floodStage) {
            this.floodStage = floodStage;
            return this;
        }

        public PercentageWatermarkSettings build() {
            return new PercentageWatermarkSettings(low, high, floodStage);
        }
    }
}
