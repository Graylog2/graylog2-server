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

import org.graylog2.logmessage.LogMessageImpl;
import org.bson.types.ObjectId;
import com.mongodb.BasicDBObject;
import org.graylog2.streams.StreamRuleImpl;
import org.junit.Test;
import static org.junit.Assert.*;

public class AdditionalFieldMatcherTest {
    @Test
    public void testTheTruthToWork() {
        assertTrue(true);
    }

    @Test
    public void testSuccessfulMatch() {
        BasicDBObject mongoRule = new BasicDBObject();
        mongoRule.put("_id", new ObjectId());
        mongoRule.put("rule_type", StreamRuleImpl.TYPE_ADDITIONAL);
        mongoRule.put("value", "foo=bar");

        StreamRuleImpl rule = new StreamRuleImpl(mongoRule);

        LogMessageImpl msg = new LogMessageImpl();
        msg.addAdditionalData("_foo", "bar");

        AdditionalFieldMatcher matcher = new AdditionalFieldMatcher();

        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testMissedMatch() {
        BasicDBObject mongoRule = new BasicDBObject();
        mongoRule.put("_id", new ObjectId());
        mongoRule.put("rule_type", StreamRuleImpl.TYPE_ADDITIONAL);
        mongoRule.put("value", "foo=bar");

        StreamRuleImpl rule = new StreamRuleImpl(mongoRule);

        LogMessageImpl msg = new LogMessageImpl();
        msg.addAdditionalData("_foo", "bazbaz");

        AdditionalFieldMatcher matcher = new AdditionalFieldMatcher();

        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testSuccessfulRegexMatch() {
        BasicDBObject mongoRule = new BasicDBObject();
        mongoRule.put("_id", new ObjectId());
        mongoRule.put("rule_type", StreamRuleImpl.TYPE_ADDITIONAL);
        mongoRule.put("value", "foo=baz|bar");

        StreamRuleImpl rule = new StreamRuleImpl(mongoRule);

        LogMessageImpl msg = new LogMessageImpl();
        msg.addAdditionalData("_foo", "bar");

        AdditionalFieldMatcher matcher = new AdditionalFieldMatcher();

        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testMissedRegexMatch() {
        BasicDBObject mongoRule = new BasicDBObject();
        mongoRule.put("_id", new ObjectId());
        mongoRule.put("rule_type", StreamRuleImpl.TYPE_ADDITIONAL);
        mongoRule.put("value", "foo=baz|bar");

        StreamRuleImpl rule = new StreamRuleImpl(mongoRule);

        LogMessageImpl msg = new LogMessageImpl();
        msg.addAdditionalData("_foo", "wat");

        AdditionalFieldMatcher matcher = new AdditionalFieldMatcher();

        assertFalse(matcher.match(msg, rule));
    }

    @Test
    public void testSuccessfulComplexButUnrealisticRegexMatch() {
        BasicDBObject mongoRule = new BasicDBObject();
        mongoRule.put("_id", new ObjectId());
        mongoRule.put("rule_type", StreamRuleImpl.TYPE_ADDITIONAL);
        mongoRule.put("value", "foo=^foo|bar\\d.+wat");

        StreamRuleImpl rule = new StreamRuleImpl(mongoRule);

        LogMessageImpl msg = new LogMessageImpl();
        msg.addAdditionalData("_foo", "bar1foowat");

        AdditionalFieldMatcher matcher = new AdditionalFieldMatcher();

        assertTrue(matcher.match(msg, rule));
    }

}