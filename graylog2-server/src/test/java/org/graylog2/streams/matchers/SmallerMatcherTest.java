/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.streams.matchers;

import org.graylog2.plugin.Message;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.plugin.streams.StreamRuleType;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.*;

public class SmallerMatcherTest extends MatcherTest {
    @Test
    public void testSuccessfulMatch() {
        StreamRule rule = getSampleRule();
        rule.setValue("100");

        Message msg = getSampleMessage();
        msg.addField("something", "20");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testSuccessfulInvertedMatch() {
        StreamRule rule = getSampleRule();
        rule.setValue("100");
        rule.setInverted(true);

        Message msg = getSampleMessage();
        msg.addField("something", "200");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testSuccessfulMatchWithNegativeValue() {
        StreamRule rule = getSampleRule();
        rule.setValue("-54354");

        Message msg = getSampleMessage();
        msg.addField("something", "-90000");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testSuccessfulInvertedMatchWithNegativeValue() {
        StreamRule rule = getSampleRule();
        rule.setValue("-54354");
        rule.setInverted(true);

        Message msg = getSampleMessage();
        msg.addField("something", "-9000");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testMissedMatch() {
        StreamRule rule = getSampleRule();
        rule.setValue("25");

        Message msg = getSampleMessage();
        msg.addField("something", "27");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testMissedInvertedMatch() {
        StreamRule rule = getSampleRule();
        rule.setValue("25");
        rule.setInverted(true);

        Message msg = getSampleMessage();
        msg.addField("something", "23");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testMissedMatchWithEqualValues() {
        StreamRule rule = getSampleRule();
        rule.setValue("-9001");

        Message msg = getSampleMessage();
        msg.addField("something", "-9001");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testSuccessfullInvertedMatchWithEqualValues() {
        StreamRule rule = getSampleRule();
        rule.setValue("-9001");
        rule.setInverted(true);

        Message msg = getSampleMessage();
        msg.addField("something", "-9001");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testMissedMatchWithInvalidValue() {
        StreamRule rule = getSampleRule();
        rule.setValue("LOL I AM NOT EVEN A NUMBER");

        Message msg = getSampleMessage();
        msg.addField("something", "-9001");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testMissedMatchWithMissingField() {
        StreamRule rule = getSampleRule();
        rule.setValue("42");

        Message msg = getSampleMessage();
        msg.addField("someother", "23");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testMissedInvertedMatchWithMissingField() {
        StreamRule rule = getSampleRule();
        rule.setValue("23");
        rule.setInverted(true);

        Message msg = getSampleMessage();
        msg.addField("someother", "42");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    protected StreamRule getSampleRule() {
        StreamRule rule = super.getSampleRule();
        rule.setType(StreamRuleType.SMALLER);

        return rule;
    }

    protected StreamRuleMatcher getMatcher(StreamRule rule) {
        StreamRuleMatcher matcher = super.getMatcher(rule);

        assertEquals(matcher.getClass(), SmallerMatcher.class);

        return matcher;
    }
}