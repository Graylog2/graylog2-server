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
public abstract class AbsoluteValueWatermarkSettings implements WatermarkSettings<ByteSize> {

    public abstract SettingsType type();

    public abstract ByteSize low();

    public abstract ByteSize high();

    @Nullable
    public abstract ByteSize floodStage();

    public static class Builder {
        private SettingsType type = SettingsType.ABSOLUTE;
        private ByteSize low;
        private ByteSize high;
        private ByteSize floodStage;

        public Builder(){}

        public Builder low(ByteSize low) {
            this.low = low;
            return this;
        }

        public Builder high(ByteSize high) {
            this.high = high;
            return this;
        }

        public Builder floodStage(ByteSize floodStage) {
            this.floodStage = floodStage;
            return this;
        }

        public AbsoluteValueWatermarkSettings build() {
            return new AutoValue_AbsoluteValueWatermarkSettings(type, low, high, floodStage);
        }
    }
}
