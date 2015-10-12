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
package org.graylog2.plugin.streams;

public enum StreamRuleType {
    EXACT(1),
    GREATER(3),
    SMALLER(4),
    REGEX(2),
    PRESENCE(5);

    private final int value;

    StreamRuleType(final int value) {
        this.value = value;
    }

    public int toInteger() {
        return value;
    }

    public static StreamRuleType fromInteger(final int numeric) {
        for (final StreamRuleType streamRuleType : StreamRuleType.values()) {
            if (streamRuleType.value == numeric) {
                return streamRuleType;
            }
        }

        return null;
    }
}
