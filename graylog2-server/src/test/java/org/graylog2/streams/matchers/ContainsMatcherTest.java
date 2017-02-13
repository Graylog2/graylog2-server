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

import java.util.Collections;

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
        rule.setInverted(true);

        msg.addField("something", "nonono");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testNonExistentField() {
        msg.addField("someother", "hello foo");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testNonExistentFieldInverted() {
        rule.setInverted(true);

        msg.addField("someother", "hello foo");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testNullFieldShouldNotMatch() {
        final String fieldName = "nullfield";
        rule.setField(fieldName);

        msg.addField(fieldName, null);

        final StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testInvertedNullFieldShouldMatch() {
        final String fieldName = "nullfield";
        rule.setField(fieldName);
        rule.setInverted(true);

        msg.addField(fieldName, null);

        final StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testSuccessfulMatchInArray() {
        msg.addField("something", Collections.singleton("foobar"));

        StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Override
    protected StreamRule getSampleRule() {
        final StreamRule rule = super.getSampleRule();
        rule.setType(StreamRuleType.CONTAINS);
        rule.setValue("foo");

        return rule;
    }

    @Override
    protected StreamRuleMatcher getMatcher(StreamRule rule) {
        final StreamRuleMatcher matcher = super.getMatcher(rule);

        assertEquals(matcher.getClass(), ContainsMatcher.class);

        return matcher;
    }
}