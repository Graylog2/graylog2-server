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
package org.graylog2.streams.matchers;

import org.graylog2.plugin.Message;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.plugin.streams.StreamRuleType;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ContainsMatcherTest extends MatcherTest {
    private StreamRule rule;
    private Message msg;

    @Before
    public void setUp() {
        rule = getSampleRule();
        msg = getSampleMessage();
    }

    @Test
    public void testSuccessfulMatch() {
        msg.addField("something", "foobar");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testMissedMatch() {
        msg.addField("something", "nonono");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testInvertedMatch() {
        final StreamRule invertedRule = rule.toBuilder().inverted(true).build();

        msg.addField("something", "nonono");

        StreamRuleMatcher matcher = getMatcher(invertedRule);
        assertTrue(matcher.match(msg, invertedRule));
    }

    @Test
    public void testNonExistentField() {
        msg.addField("someother", "hello foo");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testNonExistentFieldInverted() {
        final StreamRule invertedRule = rule.toBuilder().inverted(true).build();

        msg.addField("someother", "hello foo");

        StreamRuleMatcher matcher = getMatcher(invertedRule);
        assertTrue(matcher.match(msg, invertedRule));
    }

    @Test
    public void testNullFieldShouldNotMatch() {
        final String fieldName = "nullfield";
        final StreamRule nullFieldRule = rule.toBuilder().field(fieldName).build();

        msg.addField(fieldName, null);

        final StreamRuleMatcher matcher = getMatcher(nullFieldRule);
        assertFalse(matcher.match(msg, nullFieldRule));
    }

    @Test
    public void testInvertedNullFieldShouldMatch() {
        final String fieldName = "nullfield";
        final StreamRule invertedNullFieldRule = rule.toBuilder()
            .field(fieldName)
            .inverted(true)
            .build();

        msg.addField(fieldName, null);

        final StreamRuleMatcher matcher = getMatcher(invertedNullFieldRule);
        assertTrue(matcher.match(msg, invertedNullFieldRule));
    }

    protected StreamRule getSampleRule() {
        final StreamRule rule = super.getSampleRuleBuilder()
            .type(StreamRuleType.CONTAINS)
            .value("foo")
            .build();

        return rule;
    }

    protected StreamRuleMatcher getMatcher(StreamRule rule) {
        final StreamRuleMatcher matcher = super.getMatcher(rule);

        assertEquals(matcher.getClass(), ContainsMatcher.class);

        return matcher;
    }
}
