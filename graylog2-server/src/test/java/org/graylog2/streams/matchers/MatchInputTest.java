package org.graylog2.streams.matchers;

import org.graylog2.plugin.Message;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.plugin.streams.StreamRuleType;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MatchInputTest extends MatcherTest {

    @Test
    public void testSuccessfulMatch() {
        StreamRule rule = getSampleRule();
        rule.setValue("message-id-beef");

        Message msg = getSampleMessage();
        msg.addField(Message.FIELD_GL2_SOURCE_INPUT, "message-id-beef");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testUnsuccessfulMatch() {
        StreamRule rule = getSampleRule();
        rule.setValue("message-id-dead");

        Message msg = getSampleMessage();
        msg.addField(Message.FIELD_GL2_SOURCE_INPUT, "message-id-beef");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testUnsuccessfulMatchWhenMissing() {
        StreamRule rule = getSampleRule();
        rule.setValue("message-id-dead");

        Message msg = getSampleMessage();

        StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testSuccessfulMatchInverted() {
        StreamRule rule = getSampleRule();
        rule.setValue("message-id-beef");
        rule.setInverted(true);

        Message msg = getSampleMessage();
        msg.addField(Message.FIELD_GL2_SOURCE_INPUT, "message-id-beef");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testUnsuccessfulMatchInverted() {
        StreamRule rule = getSampleRule();
        rule.setValue("message-id-dead");
        rule.setInverted(true);

        Message msg = getSampleMessage();
        msg.addField(Message.FIELD_GL2_SOURCE_INPUT, "message-id-beef");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testUnsuccessfulMatchWhenMissingInverted() {
        StreamRule rule = getSampleRule();
        rule.setValue("message-id-dead");
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
