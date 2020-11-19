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
import org.graylog2.streams.matchers.InputMatcher;
import org.graylog2.streams.matchers.ExactMatcher;
import org.graylog2.streams.matchers.FieldPresenceMatcher;
import org.graylog2.streams.matchers.GreaterMatcher;
import org.graylog2.streams.matchers.RegexMatcher;
import org.graylog2.streams.matchers.SmallerMatcher;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StreamRuleMatcherFactoryTest {
    @Test
    public void buildReturnsCorrectStreamRuleMatcher() throws Exception {
        assertThat(StreamRuleMatcherFactory.build(StreamRuleType.EXACT)).isInstanceOf(ExactMatcher.class);
        assertThat(StreamRuleMatcherFactory.build(StreamRuleType.REGEX)).isInstanceOf(RegexMatcher.class);
        assertThat(StreamRuleMatcherFactory.build(StreamRuleType.GREATER)).isInstanceOf(GreaterMatcher.class);
        assertThat(StreamRuleMatcherFactory.build(StreamRuleType.SMALLER)).isInstanceOf(SmallerMatcher.class);
        assertThat(StreamRuleMatcherFactory.build(StreamRuleType.PRESENCE)).isInstanceOf(FieldPresenceMatcher.class);
        assertThat(StreamRuleMatcherFactory.build(StreamRuleType.ALWAYS_MATCH)).isInstanceOf(AlwaysMatcher.class);
        assertThat(StreamRuleMatcherFactory.build(StreamRuleType.MATCH_INPUT)).isInstanceOf(InputMatcher.class);
    }
}
