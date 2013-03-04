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

import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.graylog2.plugin.streams.StreamRule;
import org.junit.Test;
import static org.junit.Assert.*;

public class StreamRuleTest {

    public BasicDBObject buildMongoStreamRule() {
        BasicDBObject mongo = new BasicDBObject();
        mongo.put("_id", new ObjectId());
        mongo.put("rule_type", StreamRuleImpl.TYPE_MESSAGE);
        mongo.put("value", "bar");

        return mongo;
    }

    @Test
    public void testGetObjectId() {
        BasicDBObject mongo = this.buildMongoStreamRule();
        ObjectId objId = new ObjectId();
        mongo.put("_id", objId);

        StreamRuleImpl rule = new StreamRuleImpl(mongo);
        assertEquals(objId, rule.getObjectId());
    }

    @Test
    public void testGetRuleType() {
        StreamRuleImpl rule = new StreamRuleImpl(this.buildMongoStreamRule());
        assertEquals(StreamRuleImpl.TYPE_MESSAGE, rule.getRuleType());
    }

    @Test
    public void testGetValue() {
        StreamRuleImpl rule = new StreamRuleImpl(this.buildMongoStreamRule());
        assertEquals("bar", rule.getValue());
    }

	public static StreamRule toRule(int type, String value) {
	    BasicDBObject mongo = new BasicDBObject();
	    mongo.put("_id", new ObjectId());
	    mongo.put("rule_type", type);
	    mongo.put("value", value);
	    
		return new StreamRuleImpl(mongo);
	}

}