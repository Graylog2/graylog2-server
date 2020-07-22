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

import com.fasterxml.jackson.annotation.JsonCreator;

public enum StreamRuleType {
    EXACT(1, "match exactly", "match exactly"),
    REGEX(2, "match regular expression", "match regular expression"),
    GREATER(3, "greater than", "be greater than"),
    SMALLER(4, "smaller than", "be smaller than"),
    PRESENCE(5, "field presence", "be present"),
    CONTAINS(6, "contain", "contain"),
    ALWAYS_MATCH(7, "always match", "always match"),
    MATCH_INPUT(8, "match input", "match input");

    private final int value;
    private final String shortDesc;
    private final String longDesc;

    StreamRuleType(final int value, final String shortDesc, final String longDesc) {
        this.value = value;
        this.shortDesc = shortDesc;
        this.longDesc = longDesc;
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

    @JsonCreator
    public static StreamRuleType fromName(final String name) {
        for (final StreamRuleType streamRuleType : StreamRuleType.values()) {
            if (streamRuleType.name().equals(name)) {
                return streamRuleType;
            }
        }

        throw new IllegalArgumentException("Invalid Stream Rule Type specified: " + name);
    }

    public int getValue() {
        return value;
    }

    public String getShortDesc() {
        return shortDesc;
    }

    public String getLongDesc() {
        return longDesc;
    }
}
