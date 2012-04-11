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
import org.graylog2.streams.StreamRule;
import org.junit.Test;
import static org.junit.Assert.*;

public class HostRegexMatcherTest {
/*
    @Test
    public void testSuccessfulMatch() {
        String host1 = "foo.example.org";
        String host2 = "bar.example.com";
        String regex = "^(foo|bar)\\.example\\.(org|com)";

        BasicDBObject mongoRule = new BasicDBObject();
        mongoRule.put("_id", new ObjectId());
        mongoRule.put("rule_type", StreamRule.TYPE_HOST_REGEX);
        mongoRule.put("value",  regex);

        StreamRule rule = new StreamRule(mongoRule);
        HostRegexMatcher matcher = new HostRegexMatcher();
        GELFMessage msg = new GELFMessage();
        
        msg.setHost(host1);
        assertTrue(matcher.match(msg, rule));
        msg.setHost(host2);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testMissedMatch() {
        String host = "example.org";
        String regex = "^example\\.(com|de)";

        BasicDBObject mongoRule = new BasicDBObject();
        mongoRule.put("_id", new ObjectId());
        mongoRule.put("rule_type", StreamRule.TYPE_HOST_REGEX);
        mongoRule.put("value",  regex);

        StreamRule rule = new StreamRule(mongoRule);

        GELFMessage msg = new GELFMessage();
        msg.setHost(host);

        HostRegexMatcher matcher = new HostRegexMatcher();

        assertFalse(matcher.match(msg, rule));
    }
*/
}