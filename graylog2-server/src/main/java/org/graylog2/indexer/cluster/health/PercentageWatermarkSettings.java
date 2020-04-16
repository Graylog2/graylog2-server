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
import javax.annotation.Nullable;

@AutoValue
public abstract class PercentageWatermarkSettings implements WatermarkSettings<Double> {

    public abstract SettingsType type();

    public abstract Double low();

    public abstract Double high();

    @Nullable
    public abstract Double floodStage();

    public static class Builder {
        private SettingsType type = SettingsType.PERCENTAGE;
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
            return new AutoValue_PercentageWatermarkSettings(type, low, high, floodStage);
        }
    }
}
