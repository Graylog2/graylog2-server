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

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class FieldPresenceMatcherTest extends MatcherTest {
    @Test
    public void testBasicMatch() throws Exception {
        StreamRule rule = getSampleRule();
        rule.setField("message");
        rule.setType(StreamRuleType.PRESENCE);
        rule.setInverted(false);

        Message message = getSampleMessage();

        StreamRuleMatcher matcher = getMatcher(rule);

        Boolean result = matcher.match(message, rule);
        assertTrue(result);
    }

    @Test
    public void testBasicNonMatch() throws Exception {
        StreamRule rule = getSampleRule();
        rule.setField("nonexistentField");
        rule.setType(StreamRuleType.PRESENCE);
        rule.setInverted(false);

        Message message = getSampleMessage();

        StreamRuleMatcher matcher = getMatcher(rule);

        Boolean result = matcher.match(message, rule);
        assertFalse(result);
    }

    @Test
    public void testInvertedBasicMatch() throws Exception {
        StreamRule rule = getSampleRule();
        rule.setField("message");
        rule.setType(StreamRuleType.PRESENCE);
        rule.setInverted(true);

        Message message = getSampleMessage();

        StreamRuleMatcher matcher = getMatcher(rule);

        Boolean result = matcher.match(message, rule);
        assertFalse(result);
    }

    @Test
    public void testInvertedBasicNonMatch() throws Exception {
        StreamRule rule = getSampleRule();
        rule.setField("nonexistentField");
        rule.setType(StreamRuleType.PRESENCE);
        rule.setInverted(true);

        Message message = getSampleMessage();

        StreamRuleMatcher matcher = getMatcher(rule);

        Boolean result = matcher.match(message, rule);
        assertTrue(result);
    }

    @Test
    public void testNulledFieldNonMatch() throws Exception {
        String fieldName = "sampleField";
        StreamRule rule = getSampleRule();
        rule.setField(fieldName);
        rule.setType(StreamRuleType.PRESENCE);
        rule.setInverted(false);

        Message message = getSampleMessage();
        message.addField(fieldName, null);

        StreamRuleMatcher matcher = getMatcher(rule);

        Boolean result = matcher.match(message, rule);
        assertFalse(result);
    }

    @Test
    public void testInvertedNulledFieldMatch() throws Exception {
        String fieldName = "sampleField";
        StreamRule rule = getSampleRule();
        rule.setField(fieldName);
        rule.setType(StreamRuleType.PRESENCE);
        rule.setInverted(true);

        Message message = getSampleMessage();
        message.addField(fieldName, null);

        StreamRuleMatcher matcher = getMatcher(rule);

        Boolean result = matcher.match(message, rule);
        assertTrue(result);
    }

    @Test
    public void testEmptyStringFieldNonMatch() throws Exception {
        String fieldName = "sampleField";
        StreamRule rule = getSampleRule();
        rule.setField(fieldName);
        rule.setType(StreamRuleType.PRESENCE);
        rule.setInverted(false);

        Message message = getSampleMessage();
        message.addField(fieldName, "");

        StreamRuleMatcher matcher = getMatcher(rule);

        Boolean result = matcher.match(message, rule);
        assertFalse(result);
    }

    @Test
    public void testInvertedEmptyStringFieldMatch() throws Exception {
        String fieldName = "sampleField";
        StreamRule rule = getSampleRule();
        rule.setField(fieldName);
        rule.setType(StreamRuleType.PRESENCE);
        rule.setInverted(true);

        Message message = getSampleMessage();
        message.addField(fieldName, "");

        StreamRuleMatcher matcher = getMatcher(rule);

        Boolean result = matcher.match(message, rule);
        assertTrue(result);
    }

    @Test
    public void testRandomNumberFieldNonMatch() throws Exception {
        String fieldName = "sampleField";
        Integer randomNumber = 4;
        StreamRule rule = getSampleRule();
        rule.setField(fieldName);
        rule.setType(StreamRuleType.PRESENCE);
        rule.setInverted(false);

        Message message = getSampleMessage();
        message.addField(fieldName, randomNumber);

        StreamRuleMatcher matcher = getMatcher(rule);

        Boolean result = matcher.match(message, rule);
        assertTrue(result);
    }

    @Test
    public void testInvertedRandomNumberFieldMatch() throws Exception {
        String fieldName = "sampleField";
        Integer randomNumber = 4;
        StreamRule rule = getSampleRule();
        rule.setField(fieldName);
        rule.setType(StreamRuleType.PRESENCE);
        rule.setInverted(true);

        Message message = getSampleMessage();
        message.addField(fieldName, randomNumber);

        StreamRuleMatcher matcher = getMatcher(rule);

        Boolean result = matcher.match(message, rule);
        assertFalse(result);
    }
}
