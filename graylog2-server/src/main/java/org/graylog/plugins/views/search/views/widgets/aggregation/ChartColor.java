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
package org.graylog.plugins.views.search.views.widgets.aggregation;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonAutoDetect
public class ChartColor {
    private final String value;

    @JsonCreator
    public ChartColor(String value) {
        if (!value.startsWith("#") || !value.matches("#[a-f0-9]{3,6}")) {
            throw new IllegalArgumentException("Color value must begin with '#' and contain 3-6 hexadecimal values.");
        }
        this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
        return this.value;
    }
}
