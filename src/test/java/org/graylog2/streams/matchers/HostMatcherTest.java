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

public class HostMatcherTest {
    @Test
    public void testSuccessfulMatch() {
        StreamRule rule = StreamRuleTest.toRule(StreamRuleImpl.TYPE_HOST, "example.org");
        
        HostMatcher matcher = new HostMatcher(rule);

        LogMessage msg = new LogMessage();
        msg.setHost("example.org");

        assertTrue(matcher.match(msg));
    }

    @Test
    public void testMissedMatch() {
        StreamRule rule = StreamRuleTest.toRule(StreamRuleImpl.TYPE_HOST, "example.org");
        HostMatcher matcher = new HostMatcher(rule);

        LogMessage msg = new LogMessage();
        msg.setHost("foo.example.org");

        assertFalse(matcher.match(msg));
    }
}