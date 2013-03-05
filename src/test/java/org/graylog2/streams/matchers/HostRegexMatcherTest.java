/**
 * Copyright 2011 Lennart Koopmann <lennart@socketfeed.com>
 *
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
 *
 */

package org.graylog2.streams.matchers;

import org.graylog2.plugin.logmessage.LogMessage;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.streams.StreamRuleImpl;
import org.graylog2.streams.StreamRuleTest;
import org.junit.Test;
import static org.junit.Assert.*;

public class HostRegexMatcherTest {
    @Test
    public void testSuccessfulMatch() {
        StreamRule rule = StreamRuleTest.toRule(StreamRuleImpl.TYPE_HOST_REGEX, "^(foo|bar)\\.example\\.(org|com)");
        HostRegexMatcher matcher = new HostRegexMatcher(rule);
        
        LogMessage msg = new LogMessage();
        msg.setHost("foo.example.org");
        assertTrue(matcher.match(msg));

        msg.setHost("bar.example.com");
        assertTrue(matcher.match(msg));
    }

    @Test
    public void testMissedMatch() {
        StreamRule rule = StreamRuleTest.toRule(StreamRuleImpl.TYPE_HOST_REGEX, "^example\\.(com|de)");
        
        HostRegexMatcher matcher = new HostRegexMatcher(rule);

        LogMessage msg = new LogMessage();
        msg.setHost("example.org");

        assertFalse(matcher.match(msg));
    }
}