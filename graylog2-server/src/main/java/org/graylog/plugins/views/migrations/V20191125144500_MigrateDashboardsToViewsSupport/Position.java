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
package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

class Position {
    private static Position infinity() {
        return new Infinity();
    }

    private static Position fromInt(int value) {
        return new NumberPosition(value);
    }

    @JsonCreator
    static Position fromJson(Object value) {
        if (value instanceof Integer) {
            return fromInt((int)value);
        }
        if (value instanceof Double && value.equals(Infinity.value)) {
            return infinity();
        }
        if (value instanceof String && value.equals("Infinity")) {
            return infinity();
        }
        throw new IllegalArgumentException("Unable to deserialize " + value + " to Position.");
    }
}

class Infinity extends Position {
    static final Double value = Double.POSITIVE_INFINITY;

    Infinity() {
        super();
    }

    @JsonValue
    Double jsonValue() {
        return value;
    }
}

class NumberPosition extends Position {
    private final int value;

    NumberPosition(int value) {
        super();
        this.value = value;
    }

    @JsonValue
    int jsonValue() {
        return this.value;
    }
}
