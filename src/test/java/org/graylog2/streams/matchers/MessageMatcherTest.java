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

import org.bson.types.ObjectId;
import com.mongodb.BasicDBObject;
import org.graylog2.messagehandlers.gelf.GELFMessage;
import org.graylog2.streams.StreamRule;
import org.junit.Test;
import static org.junit.Assert.*;

public class MessageMatcherTest {

    @Test
    public void testSuccessfulMatch() {
        String message = "ohai thar|foo";
        String regex = "ohai\\sthar.+";

        BasicDBObject mongoRule = new BasicDBObject();
        mongoRule.put("_id", new ObjectId());
        mongoRule.put("rule_type", StreamRule.TYPE_MESSAGE);
        mongoRule.put("value",  regex);

        StreamRule rule = new StreamRule(mongoRule);

        GELFMessage msg = new GELFMessage();
        msg.setShortMessage(message);

        MessageMatcher matcher = new MessageMatcher();

        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testMissedMatch() {
        String message = "ohai thar|foo";
        String regex = "bar";

        BasicDBObject mongoRule = new BasicDBObject();
        mongoRule.put("_id", new ObjectId());
        mongoRule.put("rule_type", StreamRule.TYPE_MESSAGE);
        mongoRule.put("value",  regex);

        StreamRule rule = new StreamRule(mongoRule);

        GELFMessage msg = new GELFMessage();
        msg.setShortMessage(message);

        MessageMatcher matcher = new MessageMatcher();

        assertFalse(matcher.match(msg, rule));
    }

    /*
     * http://jira.graylog2.org/browse/SERVER-11
     */
    @Test
    public void testSpecificMatch1() {
        String message = "su: (to myuser) root on none";
        String regex = "(su|sudo).+";

        BasicDBObject mongoRule = new BasicDBObject();
        mongoRule.put("_id", new ObjectId());
        mongoRule.put("rule_type", StreamRule.TYPE_MESSAGE);
        mongoRule.put("value",  regex);

        StreamRule rule = new StreamRule(mongoRule);

        GELFMessage msg = new GELFMessage();
        msg.setShortMessage(message);

        MessageMatcher matcher = new MessageMatcher();

        assertTrue(matcher.match(msg, rule));
    }

    /*
     * http://jira.graylog2.org/browse/SERVER-11
     */
    @Test
    public void testSpecificMatch2() {
        String message = "MyHostname su: (to myuser) root on none\n";
        String regex = ".+su.+";

        BasicDBObject mongoRule = new BasicDBObject();
        mongoRule.put("_id", new ObjectId());
        mongoRule.put("rule_type", StreamRule.TYPE_MESSAGE);
        mongoRule.put("value",  regex);

        StreamRule rule = new StreamRule(mongoRule);

        GELFMessage msg = new GELFMessage();
        msg.setShortMessage(message);

        MessageMatcher matcher = new MessageMatcher();

        assertTrue(matcher.match(msg, rule));
    }

}