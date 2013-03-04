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

import java.util.Map;

import com.google.common.collect.Maps;
import org.graylog2.streams.StreamRuleImpl;
import org.graylog2.streams.StreamRuleTest;
import org.junit.Test;
import static org.junit.Assert.*;

public class MessageMatcherTest {
    @Test
    public void testSuccessfulMatch() {
        StreamRule rule = StreamRuleTest.toRule(StreamRuleImpl.TYPE_MESSAGE, "ohai\\sthar.+");
        
        MessageMatcher matcher = new MessageMatcher(rule);

        LogMessage msg = new LogMessage();
        msg.setShortMessage("ohai thar|foo");

        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testMissedMatch() {
        StreamRule rule = StreamRuleTest.toRule(StreamRuleImpl.TYPE_MESSAGE, "bar");

        MessageMatcher matcher = new MessageMatcher(rule);
        
        LogMessage msg = new LogMessage();
        msg.setShortMessage("ohai thar|foo");

        assertFalse(matcher.match(msg, rule));
    }

    /*
     * Testing specific cases reported by users.
     */
    @Test
    public void testSpecificMatches() {
        Map<String, String> cases = Maps.newHashMap();

        cases.put("su: (to myuser) root on none", "(su|sudo).+"); // http://jira.graylog2.org/browse/SERVER-11
        cases.put("MyHostname su: (to myuser) root on none\n", ".+su.+"); // http://jira.graylog2.org/browse/SERVER-11
        cases.put("aws.ses.blacklist[3648]: Received error response: " // https://groups.google.com/forum/#!topic/graylog2/k2c83gtwqbk
                + "Status Code: 400, AWS Request ID: bbbcd5c8-5d70-11"
                + "e0-93c0-07085af79fd6, AWS Error Code: MessageRejec"
                + "ted, AWS Error Message: Address blacklisted.", ".+(?i).Received error response.+Address blacklisted.+");

        for (Map.Entry<String, String> e : cases.entrySet()) {
            StreamRule rule = StreamRuleTest.toRule(StreamRuleImpl.TYPE_MESSAGE, e.getValue());

            MessageMatcher matcher = new MessageMatcher(rule);

            LogMessage msg = new LogMessage();
            msg.setShortMessage(e.getKey());

            assertTrue(matcher.match(msg, rule));
        }
    }
}