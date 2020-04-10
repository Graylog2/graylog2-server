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

import javax.annotation.Nullable;

public class AbsoluteValueWatermarkSettings implements WatermarkSettings<ByteSizeValue> {

    private ByteSizeValue low;

    private ByteSizeValue high;

    @Nullable
    private ByteSizeValue floodStage;

    private AbsoluteValueWatermarkSettings(ByteSizeValue low, ByteSizeValue high, ByteSizeValue floodStage) {
        this.low = low;
        this.high = high;
        this.floodStage = floodStage;
    }

    @Override
    public SettingsType type() {
        return SettingsType.ABSOLUTE;
    }

    @Override
    public ByteSizeValue low() {
        return low;
    }

    @Override
    public ByteSizeValue high() {
        return high;
    }

    @Override
    public ByteSizeValue floodStage() {
        return floodStage;
    }

    public static class Builder {
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
            return new AbsoluteValueWatermarkSettings(low, high, floodStage);
        }
    }
}
