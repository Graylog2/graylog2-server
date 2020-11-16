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

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RegexMatcherTest extends MatcherTest {

    @Test
    public void testSuccessfulMatch() {
        StreamRule rule = getSampleRule();
        rule.setValue("^foo");

        Message msg = getSampleMessage();
        msg.addField("something", "foobar");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testSuccessfulInvertedMatch() {
        StreamRule rule = getSampleRule();
        rule.setValue("^foo");
        rule.setInverted(true);

        Message msg = getSampleMessage();
        msg.addField("something", "zomg");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testMissedMatch() {
        StreamRule rule = getSampleRule();
        rule.setValue("^foo");

        Message msg = getSampleMessage();
        msg.addField("something", "zomg");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testMissedInvertedMatch() {
        StreamRule rule = getSampleRule();
        rule.setValue("^foo");
        rule.setInverted(true);

        Message msg = getSampleMessage();
        msg.addField("something", "foobar");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testMissingFieldShouldNotMatch() throws Exception {
        final StreamRule rule = getSampleRule();
        rule.setField("nonexistingfield");
        rule.setValue("^foo");

        final Message msg = getSampleMessage();

        final StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testInvertedMissingFieldShouldMatch() throws Exception {
        final StreamRule rule = getSampleRule();
        rule.setField("nonexistingfield");
        rule.setValue("^foo");
        rule.setInverted(true);

        final Message msg = getSampleMessage();

        final StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testNullFieldShouldNotMatch() throws Exception {
        final String fieldName = "nullfield";
        final StreamRule rule = getSampleRule();
        rule.setField(fieldName);
        rule.setValue("^foo");

        final Message msg = getSampleMessage();
        msg.addField(fieldName, null);

        final StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testInvertedNullFieldShouldMatch() throws Exception {
        final String fieldName = "nullfield";
        final StreamRule rule = getSampleRule();
        rule.setField(fieldName);
        rule.setValue("^foo");
        rule.setInverted(true);

        final Message msg = getSampleMessage();
        msg.addField(fieldName, null);

        final StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testSuccessfulComplexRegexMatch() {
        StreamRule rule = getSampleRule();
        rule.setField("some_field");
        rule.setValue("foo=^foo|bar\\d.+wat");

        Message msg = getSampleMessage();
        msg.addField("some_field", "bar1foowat");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testSuccessfulMatchInArray() {
        StreamRule rule = getSampleRule();
        rule.setValue("foobar");

        Message msg = getSampleMessage();
        msg.addField("something", Collections.singleton("foobar"));

        StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Override
    protected StreamRule getSampleRule() {
        StreamRule rule = super.getSampleRule();
        rule.setType(StreamRuleType.REGEX);

        return rule;
    }

    @Override
    protected StreamRuleMatcher getMatcher(StreamRule rule) {
        StreamRuleMatcher matcher = super.getMatcher(rule);

        assertEquals(matcher.getClass(), RegexMatcher.class);

        return matcher;
    }
}
