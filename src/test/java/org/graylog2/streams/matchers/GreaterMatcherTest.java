/**
 * Copyright 2011, 2012, 2013 Lennart Koopmann <lennart@socketfeed.com>
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

import org.graylog2.plugin.Message;
import org.bson.types.ObjectId;
import com.mongodb.BasicDBObject;
import org.graylog2.streams.StreamRuleImpl;
import org.junit.Test;
import static org.junit.Assert.*;

public class GreaterMatcherTest {

    @Test
    public void testSuccessfulMatch() {
        BasicDBObject mongoRule = new BasicDBObject();
        mongoRule.put("_id", new ObjectId());
        mongoRule.put("rule_type", StreamRuleImpl.TYPE_GREATER);
        mongoRule.put("field", "something");
        mongoRule.put("value", "3");

        StreamRuleImpl rule = new StreamRuleImpl(mongoRule);

        Message msg = new Message("foo", "bar", 0);
        msg.addField("something", "4");

        GreaterMatcher matcher = new GreaterMatcher();
        assertTrue(matcher.match(msg, rule));
    }
    
    @Test
    public void testSuccessfulMatchWithNegativeValue() {
        BasicDBObject mongoRule = new BasicDBObject();
        mongoRule.put("_id", new ObjectId());
        mongoRule.put("rule_type", StreamRuleImpl.TYPE_GREATER);
        mongoRule.put("field", "something");
        mongoRule.put("value", "-54354");

        StreamRuleImpl rule = new StreamRuleImpl(mongoRule);

        Message msg = new Message("foo", "bar", 0);
        msg.addField("something", "4");

        GreaterMatcher matcher = new GreaterMatcher();
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testMissedMatch() {
        BasicDBObject mongoRule = new BasicDBObject();
        mongoRule.put("_id", new ObjectId());
        mongoRule.put("rule_type", StreamRuleImpl.TYPE_GREATER);
        mongoRule.put("field", "something");
        mongoRule.put("value", "25");

        StreamRuleImpl rule = new StreamRuleImpl(mongoRule);

        Message msg = new Message("foo", "bar", 0);
        msg.addField("something", "12");

        GreaterMatcher matcher = new GreaterMatcher();
        assertFalse(matcher.match(msg, rule));
    }
    
    @Test
    public void testMissedMatchWithEqualValues() {
        BasicDBObject mongoRule = new BasicDBObject();
        mongoRule.put("_id", new ObjectId());
        mongoRule.put("rule_type", StreamRuleImpl.TYPE_GREATER);
        mongoRule.put("field", "something");
        mongoRule.put("value", "-9001");

        StreamRuleImpl rule = new StreamRuleImpl(mongoRule);

        Message msg = new Message("foo", "bar", 0);
        msg.addField("something", "-9001");

        GreaterMatcher matcher = new GreaterMatcher();
        assertFalse(matcher.match(msg, rule));
    }
    
    @Test
    public void testMissedMatchWithInvalidValue() {
        BasicDBObject mongoRule = new BasicDBObject();
        mongoRule.put("_id", new ObjectId());
        mongoRule.put("rule_type", StreamRuleImpl.TYPE_GREATER);
        mongoRule.put("field", "something");
        mongoRule.put("value", "LOL I AM NOT EVEN A NUMBER");

        StreamRuleImpl rule = new StreamRuleImpl(mongoRule);

        Message msg = new Message("foo", "bar", 0);
        msg.addField("something", "90000");

        GreaterMatcher matcher = new GreaterMatcher();
        assertFalse(matcher.match(msg, rule));
    }

}