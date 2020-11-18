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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InputMatcherTest extends MatcherTest {

    @Test
    public void testSuccessfulMatch() {
        StreamRule rule = getSampleRule();
        rule.setValue("input-id-beef");

        Message msg = getSampleMessage();
        msg.addField(Message.FIELD_GL2_SOURCE_INPUT, "input-id-beef");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testUnsuccessfulMatch() {
        StreamRule rule = getSampleRule();
        rule.setValue("input-id-dead");

        Message msg = getSampleMessage();
        msg.addField(Message.FIELD_GL2_SOURCE_INPUT, "input-id-beef");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testUnsuccessfulMatchWhenMissing() {
        StreamRule rule = getSampleRule();
        rule.setValue("input-id-dead");

        Message msg = getSampleMessage();

        StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testSuccessfulMatchInverted() {
        StreamRule rule = getSampleRule();
        rule.setValue("input-id-beef");
        rule.setInverted(true);

        Message msg = getSampleMessage();
        msg.addField(Message.FIELD_GL2_SOURCE_INPUT, "input-id-beef");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testUnsuccessfulMatchInverted() {
        StreamRule rule = getSampleRule();
        rule.setValue("input-id-dead");
        rule.setInverted(true);

        Message msg = getSampleMessage();
        msg.addField(Message.FIELD_GL2_SOURCE_INPUT, "input-id-beef");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testUnsuccessfulMatchWhenMissingInverted() {
        StreamRule rule = getSampleRule();
        rule.setValue("input-id-dead");
        rule.setInverted(true);

        Message msg = getSampleMessage();

        StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Override
    protected StreamRule getSampleRule() {
        StreamRule rule = super.getSampleRule();
        rule.setType(StreamRuleType.MATCH_INPUT);

        return rule;
    }

}
