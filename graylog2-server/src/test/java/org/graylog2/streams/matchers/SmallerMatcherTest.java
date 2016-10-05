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
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SmallerMatcherTest extends MatcherTest {
    @Test
    public void testSuccessfulMatch() {
        StreamRule rule = getSampleRule().toBuilder()
            .value("100")
            .build();

        Message msg = getSampleMessage();
        msg.addField("something", "20");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testSuccessfulDoubleMatch() {
        StreamRule rule = getSampleRule().toBuilder()
            .value("100")
            .build();

        Message msg = getSampleMessage();
        msg.addField("something", "20.45");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testSuccessfulInvertedMatch() {
        StreamRule rule = getSampleRule().toBuilder()
            .value("100")
            .inverted(true)
            .build();

        Message msg = getSampleMessage();
        msg.addField("something", "200");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testSuccessfulMatchWithNegativeValue() {
        StreamRule rule = getSampleRule().toBuilder()
            .value("-54354")
            .build();

        Message msg = getSampleMessage();
        msg.addField("something", "-90000");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testSuccessfulDoubleMatchWithNegativeValue() {
        StreamRule rule = getSampleRule().toBuilder()
            .value("-54354.42")
            .build();

        Message msg = getSampleMessage();
        msg.addField("something", "-90000.12");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testSuccessfulInvertedMatchWithNegativeValue() {
        StreamRule rule = getSampleRule().toBuilder()
            .value("-54354")
            .inverted(true)
            .build();

        Message msg = getSampleMessage();
        msg.addField("something", "-9000");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testMissedMatch() {
        StreamRule rule = getSampleRule().toBuilder()
            .value("25")
            .build();

        Message msg = getSampleMessage();
        msg.addField("something", "27");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testMissedDoubleMatch() {
        StreamRule rule = getSampleRule().toBuilder()
            .value("25")
            .build();

        Message msg = getSampleMessage();
        msg.addField("something", "27.45");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testMissedInvertedMatch() {
        StreamRule rule = getSampleRule().toBuilder()
            .value("25")
            .inverted(true)
            .build();

        Message msg = getSampleMessage();
        msg.addField("something", "23");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testMissedMatchWithEqualValues() {
        StreamRule rule = getSampleRule().toBuilder()
            .value("-9001")
            .build();

        Message msg = getSampleMessage();
        msg.addField("something", "-9001");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testMissedDoubleMatchWithEqualValues() {
        StreamRule rule = getSampleRule().toBuilder()
            .value("-9001.23")
            .build();

        Message msg = getSampleMessage();
        msg.addField("something", "-9001.23");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testSuccessfullInvertedMatchWithEqualValues() {
        StreamRule rule = getSampleRule().toBuilder()
            .value("-9001")
            .inverted(true)
            .build();

        Message msg = getSampleMessage();
        msg.addField("something", "-9001");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testMissedMatchWithInvalidValue() {
        StreamRule rule = getSampleRule().toBuilder()
            .value("LOL I AM NOT EVEN A NUMBER")
            .build();

        Message msg = getSampleMessage();
        msg.addField("something", "-9001");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testMissedDoubleMatchWithInvalidValue() {
        StreamRule rule = getSampleRule().toBuilder()
            .value("LOL I AM NOT EVEN A NUMBER")
            .build();

        Message msg = getSampleMessage();
        msg.addField("something", "-9001.42");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testMissedMatchWithMissingField() {
        StreamRule rule = getSampleRule().toBuilder()
            .value("42")
            .build();

        Message msg = getSampleMessage();
        msg.addField("someother", "23");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testMissedInvertedMatchWithMissingField() {
        StreamRule rule = getSampleRule().toBuilder()
            .value("23")
            .inverted(true)
            .build();

        Message msg = getSampleMessage();
        msg.addField("someother", "42");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    protected StreamRule getSampleRule() {
        return super.getSampleRuleBuilder()
            .type(StreamRuleType.SMALLER)
            .build();
    }

    protected StreamRuleMatcher getMatcher(StreamRule rule) {
        StreamRuleMatcher matcher = super.getMatcher(rule);

        assertEquals(matcher.getClass(), SmallerMatcher.class);

        return matcher;
    }
}
