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
package org.graylog.plugins.netflow.v9;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@JsonAutoDetect
@AutoValue
public abstract class NetFlowV9FieldType {
    @JsonProperty("id")
    public abstract int id();

    @JsonProperty("value_type")
    public abstract ValueType valueType();

    @JsonProperty("name")
    public abstract String name();

    @JsonCreator
    public static NetFlowV9FieldType create(@JsonProperty("id") int id,
                                            @JsonProperty("value_type") ValueType valueType,
                                            @JsonProperty("name") String name) {
        return new AutoValue_NetFlowV9FieldType(id, valueType, name);
    }

    public enum ValueType {
        UINT8(1), INT8(1), UINT16(2), INT16(2), UINT24(3), INT24(3),
        UINT32(4), INT32(4), UINT64(8), INT64(8), IPV4(4), IPV6(16),
        MAC(6), STRING(0), VARINT(0);

        private final int defaultLength;

        ValueType(int defaultLength) {
            this.defaultLength = defaultLength;
        }

        public int getDefaultLength() {
            return defaultLength;
        }

        public static ValueType byLength(int length) {
            switch (length) {
                case 1: return UINT8;
                case 2: return UINT16;
                case 3: return UINT24;
                case 4: return UINT32;
                case 6: return MAC;
                case 8: return UINT64;
                case 16: return IPV6;
                default: return VARINT;
            }
        }
    }
}
