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
package org.graylog2.streams;

import org.graylog2.plugin.streams.StreamRuleType;
import org.graylog2.streams.matchers.AlwaysMatcher;
import org.graylog2.streams.matchers.MatchInput;
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
        assertThat(StreamRuleMatcherFactory.build(StreamRuleType.MATCH_INPUT)).isInstanceOf(MatchInput.class);
    }
}
