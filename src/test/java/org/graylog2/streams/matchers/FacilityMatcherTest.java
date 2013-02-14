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
import org.graylog2.plugin.Tools;
import org.bson.types.ObjectId;
import com.mongodb.BasicDBObject;
import org.graylog2.streams.StreamRuleImpl;
import org.junit.Test;
import static org.junit.Assert.*;

public class FacilityMatcherTest {

    @Test
    public void testTheTruthToWork() {
        assertTrue(true);
    }

    @Test
    public void testSuccessfulMatch() {
        String facility = "foobar";

        BasicDBObject mongoRule = new BasicDBObject();
        mongoRule.put("_id", new ObjectId());
        mongoRule.put("rule_type", StreamRuleImpl.TYPE_FACILITY);
        mongoRule.put("value", facility);

        StreamRuleImpl rule = new StreamRuleImpl(mongoRule);

        LogMessage msg = new LogMessage();
        msg.setFacility(facility);

        FacilityMatcher matcher = new FacilityMatcher();

        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testMissedMatch() {
        BasicDBObject mongoRule = new BasicDBObject();
        mongoRule.put("_id", new ObjectId());
        mongoRule.put("rule_type", StreamRuleImpl.TYPE_FACILITY);
        mongoRule.put("value", "foobar");

        StreamRuleImpl rule = new StreamRuleImpl(mongoRule);

        LogMessage msg = new LogMessage();
        msg.setFacility("barfoo");

        FacilityMatcher matcher = new FacilityMatcher();

        assertFalse(matcher.match(msg, rule));
    }

}