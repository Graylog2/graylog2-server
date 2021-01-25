/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
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
