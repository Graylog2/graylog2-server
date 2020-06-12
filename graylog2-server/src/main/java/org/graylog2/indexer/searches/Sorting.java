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
package org.graylog2.indexer.searches;

import org.graylog2.plugin.Message;

import java.util.Locale;

public class Sorting {

    public static final Sorting DEFAULT = new Sorting(Message.FIELD_TIMESTAMP, Direction.DESC);

    public enum Direction {
        ASC,
        DESC
    }

    private final String field;
    private final Direction direction;

    public Sorting(String field, Direction direction) {
        this.field = field;
        this.direction = direction;
    }

    public String getField() {
        return field;
    }

    public Direction getDirection() { return this.direction; }

    public static Sorting fromApiParam(String param) {
        if (param == null || !param.contains(":")) {
            throw new IllegalArgumentException("Invalid sorting parameter: " + param);
        }

        String[] parts = param.split(":");

        return new Sorting(parts[0], Direction.valueOf(parts[1].toUpperCase(Locale.ENGLISH)));
    }

}
