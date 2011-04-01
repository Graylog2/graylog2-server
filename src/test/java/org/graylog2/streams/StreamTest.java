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

package org.graylog2.streams;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.junit.Test;
import static org.junit.Assert.*;

public class StreamTest {

    public BasicDBObject buildMongoStream() {
        BasicDBObject mongo = new BasicDBObject();
        mongo.put("_id", new ObjectId());
        mongo.put("title", "foo");

        return mongo;
    }

    @Test
    public void testGetStreamRules() {
        BasicDBObject mongoRule1 = new BasicDBObject();
        mongoRule1.put("_id", new ObjectId());
        mongoRule1.put("rule_type", StreamRule.TYPE_HOST);
        mongoRule1.put("value", "example.org");

        BasicDBObject mongoRule2 = new BasicDBObject();
        mongoRule2.put("_id", new ObjectId());
        mongoRule2.put("rule_type", StreamRule.TYPE_SEVERITY);
        mongoRule2.put("value", "2");

        BasicDBList rules = new BasicDBList();
        rules.add(mongoRule1);
        rules.add(mongoRule2);

        BasicDBObject mongo = this.buildMongoStream();
        mongo.put("streamrules", rules);

        Stream stream = new Stream(mongo);

        assertEquals(2, stream.getStreamRules().size());

        assertEquals(StreamRule.TYPE_HOST, stream.getStreamRules().get(0).getRuleType());
        assertEquals("example.org", stream.getStreamRules().get(0).getValue());

        assertEquals(StreamRule.TYPE_SEVERITY, stream.getStreamRules().get(1).getRuleType());
        assertEquals("2", stream.getStreamRules().get(1).getValue());
    }

    @Test
    public void testGetStreamRulesWithStreamThatHasNoRules() {
        BasicDBObject mongo = this.buildMongoStream();
        mongo.put("streamrules", null);

        Stream stream = new Stream(mongo);

        assertEquals(0, stream.getStreamRules().size());

        // All is fine if there are no exceptions thrown and we get here.
    }

    @Test
    public void testGetId() {
        BasicDBObject mongo = this.buildMongoStream();
        Stream stream = new Stream(mongo);

        assertEquals(mongo.get("_id"), stream.getId());
    }

    @Test
    public void testGetTitle() {
        Stream stream = new Stream(this.buildMongoStream());

        assertEquals("foo", stream.getTitle());
    }

}