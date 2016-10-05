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

public class RegexMatcherTest extends MatcherTest {

    @Test
    public void testSuccessfulMatch() {
        StreamRule rule = getSampleRule().toBuilder()
            .value("^foo")
            .build();

        Message msg = getSampleMessage();
        msg.addField("something", "foobar");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testSuccessfulInvertedMatch() {
        StreamRule rule = getSampleRule().toBuilder()
            .value("^foo")
            .inverted(true)
            .build();

        Message msg = getSampleMessage();
        msg.addField("something", "zomg");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testMissedMatch() {
        StreamRule rule = getSampleRule().toBuilder()
            .value("^foo")
            .build();

        Message msg = getSampleMessage();
        msg.addField("something", "zomg");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testMissedInvertedMatch() {
        StreamRule rule = getSampleRule().toBuilder()
            .value("^foo")
            .inverted(true)
            .build();

        Message msg = getSampleMessage();
        msg.addField("something", "foobar");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testMissingFieldShouldNotMatch() throws Exception {
        final StreamRule rule = getSampleRule().toBuilder()
            .field("nonexistingfield")
            .value("^foo")
            .build();

        final Message msg = getSampleMessage();

        final StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testInvertedMissingFieldShouldMatch() throws Exception {
        final StreamRule rule = getSampleRule().toBuilder()
            .field("nonexistingfield")
            .value("^foo")
            .inverted(true)
            .build();

        final Message msg = getSampleMessage();

        final StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testNullFieldShouldNotMatch() throws Exception {
        final String fieldName = "nullfield";
        final StreamRule rule = getSampleRule().toBuilder()
            .field(fieldName)
            .value("^foo")
            .build();

        final Message msg = getSampleMessage();
        msg.addField(fieldName, null);

        final StreamRuleMatcher matcher = getMatcher(rule);
        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testInvertedNullFieldShouldMatch() throws Exception {
        final String fieldName = "nullfield";
        final StreamRule rule = getSampleRule().toBuilder()
            .field(fieldName)
            .value("^foo")
            .inverted(true)
            .build();

        final Message msg = getSampleMessage();
        msg.addField(fieldName, null);

        final StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testSuccessfulComplexRegexMatch() {
        StreamRule rule = getSampleRule().toBuilder()
            .field("some_field")
            .value("foo=^foo|bar\\d.+wat")
            .build();

        Message msg = getSampleMessage();
        msg.addField("some_field", "bar1foowat");

        StreamRuleMatcher matcher = getMatcher(rule);
        assertTrue(matcher.match(msg, rule));
    }

    protected StreamRule getSampleRule() {
        return super.getSampleRuleBuilder()
            .type(StreamRuleType.REGEX)
            .build();
    }

    protected StreamRuleMatcher getMatcher(StreamRule rule) {
        StreamRuleMatcher matcher = super.getMatcher(rule);

        assertEquals(matcher.getClass(), RegexMatcher.class);

        return matcher;
    }
}
