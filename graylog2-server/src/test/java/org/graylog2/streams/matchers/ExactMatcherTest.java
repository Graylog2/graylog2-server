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

public class ExactMatcherTest extends MatcherTest {

    @Test
    public void testSuccessfulMatch() {
        StreamRule rule = getSampleRule();

        Message msg = getSampleMessage();
        msg.addField("something", "foo");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testMissedMatch() {
        StreamRule rule = getSampleRule();

        Message msg = getSampleMessage();
        msg.addField("something", "nonono");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testInvertedMatch() {
        StreamRule rule = getSampleRule();
        rule.setInverted(true);

        Message msg = getSampleMessage();
        msg.addField("something", "nonono");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testNonExistantField() {
        StreamRule rule = getSampleRule();

        Message msg = getSampleMessage();
        msg.addField("someother", "foo");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testNonExistantFieldInverted() {
        StreamRule rule = getSampleRule();
        rule.setInverted(true);

        Message msg = getSampleMessage();
        msg.addField("someother", "foo");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testNullFieldShouldNotMatch() {
        final String fieldName = "nullfield";
        final StreamRule rule = getSampleRule();
        rule.setField(fieldName);

        final Message msg = getSampleMessage();
        msg.addField(fieldName, null);

        final StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testInvertedNullFieldShouldMatch() {
        final String fieldName = "nullfield";
        final StreamRule rule = getSampleRule();
        rule.setField(fieldName);
        rule.setInverted(true);

        final Message msg = getSampleMessage();
        msg.addField(fieldName, null);

        final StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Override
    protected StreamRule getSampleRule() {
        StreamRule rule = super.getSampleRule();
        rule.setType(StreamRuleType.EXACT);
        rule.setValue("foo");

        return rule;
    }

    @Override
    protected StreamRuleMatcher getMatcher(StreamRule rule) {
        StreamRuleMatcher matcher = super.getMatcher(rule);

        assertEquals(matcher.getClass(), ExactMatcher.class);

        return matcher;
    }

}
