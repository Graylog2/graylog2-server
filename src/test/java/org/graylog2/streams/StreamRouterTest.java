/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
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
package org.graylog2.streams;

import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.graylog2.plugin.logmessage.LogMessage;
import org.graylog2.streams.matchers.FacilityMatcher;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class StreamRouterTest {

    // Matchers themselves are tested in own tests.

    @Test
         public void testMatchStreamRule() throws Exception {
        String facility = "somefacility";

        LogMessage msg = new LogMessage();
        msg.setShortMessage("hello");
        msg.setHost("foo");
        msg.setFacility(facility);

        BasicDBObject mongoRule = new BasicDBObject();
        mongoRule.put("_id", new ObjectId());
        mongoRule.put("rule_type", StreamRuleImpl.TYPE_FACILITY);
        mongoRule.put("value", facility);

        StreamRuleImpl rule = new StreamRuleImpl(mongoRule);

        StreamRouter sr = new StreamRouter();
        assertTrue(sr.matchStreamRule(msg, new FacilityMatcher(), rule));
    }

    @Test
    public void testMatchInvertedStreamRule() throws Exception {
        String facility = "somefacility";

        LogMessage msg = new LogMessage();
        msg.setShortMessage("hello");
        msg.setHost("foo");
        msg.setFacility(facility);

        BasicDBObject mongoRule = new BasicDBObject();
        mongoRule.put("_id", new ObjectId());
        mongoRule.put("rule_type", StreamRuleImpl.TYPE_FACILITY);
        mongoRule.put("value", facility);
        mongoRule.put("inverted", true);

        StreamRuleImpl rule = new StreamRuleImpl(mongoRule);

        StreamRouter sr = new StreamRouter();
        assertFalse(sr.matchStreamRule(msg, new FacilityMatcher(), rule));
    }

}
