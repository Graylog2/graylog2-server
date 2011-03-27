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

import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.graylog2.messagehandlers.gelf.GELFMessage;
import org.graylog2.streams.StreamRule;
import org.junit.Test;
import static org.junit.Assert.*;

public class HostMatcherTest {

    @Test
    public void testSuccessfulMatch() {
        String host = "example.org";

        BasicDBObject mongoRule = new BasicDBObject();
        mongoRule.put("_id", new ObjectId());
        mongoRule.put("rule_type", StreamRule.TYPE_HOST);
        mongoRule.put("value", host);

        StreamRule rule = new StreamRule(mongoRule);

        GELFMessage msg = new GELFMessage();
        msg.setHost(host);

        HostMatcher matcher = new HostMatcher();

        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testMissedMatch() {
        String host = "example.org";

        BasicDBObject mongoRule = new BasicDBObject();
        mongoRule.put("_id", new ObjectId());
        mongoRule.put("rule_type", StreamRule.TYPE_HOST);
        mongoRule.put("value", host);

        StreamRule rule = new StreamRule(mongoRule);

        GELFMessage msg = new GELFMessage();
        msg.setHost(host + "foo");

        HostMatcher matcher = new HostMatcher();

        assertFalse(matcher.match(msg, rule));
    }


}