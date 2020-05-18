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
