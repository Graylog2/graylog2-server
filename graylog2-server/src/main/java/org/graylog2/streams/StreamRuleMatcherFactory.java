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
package org.graylog2.streams;

import org.graylog2.plugin.streams.StreamRuleType;
import org.graylog2.streams.matchers.AlwaysMatcher;
import org.graylog2.streams.matchers.ContainsMatcher;
import org.graylog2.streams.matchers.ExactMatcher;
import org.graylog2.streams.matchers.FieldPresenceMatcher;
import org.graylog2.streams.matchers.GreaterMatcher;
import org.graylog2.streams.matchers.InputMatcher;
import org.graylog2.streams.matchers.RegexMatcher;
import org.graylog2.streams.matchers.SmallerMatcher;
import org.graylog2.streams.matchers.StreamRuleMatcher;

public class StreamRuleMatcherFactory {
    public static StreamRuleMatcher build(StreamRuleType ruleType) throws InvalidStreamRuleTypeException {
        switch (ruleType) {
            case EXACT:
                return new ExactMatcher();
            case REGEX:
                return new RegexMatcher();
            case GREATER:
                return new GreaterMatcher();
            case SMALLER:
                return new SmallerMatcher();
            case PRESENCE:
                return new FieldPresenceMatcher();
            case CONTAINS:
                return new ContainsMatcher();
            case ALWAYS_MATCH:
                return new AlwaysMatcher();
            case MATCH_INPUT:
                return new InputMatcher();
            default:
                throw new InvalidStreamRuleTypeException();
        }
    }
}
