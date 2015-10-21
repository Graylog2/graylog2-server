/**
 * The MIT License
 * Copyright (c) 2012 Graylog, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.plugin.streams;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;

public enum StreamRuleType {
    EXACT(1, "match exactly", "match exactly"),
    REGEX(2, "match regular expression", "match regular expression"),
    GREATER(3, "greater than", "be greater than"),
    SMALLER(4, "smaller than", "be smaller than"),
    PRESENCE(5, "field presence", "be present");

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

    public static StreamRuleType fromInteger(@JsonProperty("value") final int numeric) {
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

    public Map<String, Object> toMap() {
        final Map<String, Object> result = ImmutableMap.<String, Object>of(
                "id", value,
                "name", name(),
                "short_desc", shortDesc,
                "long_desc", longDesc
        );

        return result;
    }
}
