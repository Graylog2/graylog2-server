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
public abstract class AbsoluteValueWatermarkSettings implements WatermarkSettings<ByteSizeValue> {

    public abstract SettingsType type();

    public abstract ByteSizeValue low();

    public abstract ByteSizeValue high();

    @Nullable
    public abstract ByteSizeValue floodStage();

    public static class Builder {
        private SettingsType type = SettingsType.ABSOLUTE;
        private ByteSizeValue low;
        private ByteSizeValue high;
        private ByteSizeValue floodStage;

        public Builder(){}

        public Builder low(ByteSizeValue low) {
            this.low = low;
            return this;
        }

        public Builder high(ByteSizeValue high) {
            this.high = high;
            return this;
        }

        public Builder floodStage(ByteSizeValue floodStage) {
            this.floodStage = floodStage;
            return this;
        }

        public AbsoluteValueWatermarkSettings build() {
            return new AutoValue_AbsoluteValueWatermarkSettings(type, low, high, floodStage);
        }
    }
}
