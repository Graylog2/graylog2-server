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
package org.graylog2.streams.matchers;

import org.graylog2.plugin.Message;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.plugin.streams.StreamRuleType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GreaterMatcherTest extends MatcherTest {

    @Test
    public void testSuccessfulMatch() {
        StreamRule rule = getSampleRule();
        rule.setValue("3");

        Message msg = getSampleMessage();
        msg.addField("something", "4");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testSuccessfulDoubleMatch() {
        StreamRule rule = getSampleRule();
        rule.setValue("1.0");

        Message msg = getSampleMessage();
        msg.addField("something", "1.1");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testSuccessfulMatchWithNegativeValue() {
        StreamRule rule = getSampleRule();
        rule.setValue("-54354");

        Message msg = getSampleMessage();
        msg.addField("something", "4");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testSuccessfulDoubleMatchWithNegativeValue() {
        StreamRule rule = getSampleRule();
        rule.setValue("-54354.0");

        Message msg = getSampleMessage();
        msg.addField("something", "4.1");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testSuccessfullInvertedMatch() {
        StreamRule rule = getSampleRule();
        rule.setValue("10");
        rule.setInverted(true);

        Message msg = getSampleMessage();
        msg.addField("something", "4");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testMissedMatch() {
        StreamRule rule = getSampleRule();
        rule.setValue("25");

        Message msg = getSampleMessage();
        msg.addField("something", "12");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testMissedDoubleMatch() {
        StreamRule rule = getSampleRule();
        rule.setValue("25");

        Message msg = getSampleMessage();
        msg.addField("something", "12.4");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testMissedInvertedMatch() {
        StreamRule rule = getSampleRule();
        rule.setValue("25");
        rule.setInverted(true);

        Message msg = getSampleMessage();
        msg.addField("something", "30");

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
    public void testMissedDoubleMatchWithEqualValues() {
        StreamRule rule = getSampleRule();
        rule.setValue("-9001.45");

        Message msg = getSampleMessage();
        msg.addField("something", "-9001.45");

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
        msg.addField("something", "90000");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testMissedDoubleMatchWithInvalidValue() {
        StreamRule rule = getSampleRule();
        rule.setValue("LOL I AM NOT EVEN A NUMBER");

        Message msg = getSampleMessage();
        msg.addField("something", "90000.23");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testMissedMatchMissingField() {
        StreamRule rule = getSampleRule();
        rule.setValue("42");

        Message msg = getSampleMessage();
        msg.addField("someother", "50");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testMissedInvertedMatchMissingField() {
        StreamRule rule = getSampleRule();
        rule.setValue("42");
        rule.setInverted(true);

        Message msg = getSampleMessage();
        msg.addField("someother", "30");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Override
    protected StreamRule getSampleRule() {
        StreamRule rule = super.getSampleRule();
        rule.setType(StreamRuleType.GREATER);

        return rule;
    }

    @Override
    protected StreamRuleMatcher getMatcher(StreamRule rule) {
        StreamRuleMatcher matcher = super.getMatcher(rule);

        assertEquals(matcher.getClass(), GreaterMatcher.class);

        return matcher;
    }
}
