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


package org.graylog2.blacklists;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.junit.Test;
import static org.junit.Assert.*;

public class BlacklistTest {

    public BasicDBObject buildMongoBlacklist() {
        BasicDBObject mongo = new BasicDBObject();
        mongo.put("_id", new ObjectId());
        mongo.put("title", "foo");

        return mongo;
    }

    @Test
    public void testGetStreamRules() {
        BasicDBObject mongoList = this.buildMongoBlacklist();

        BasicDBObject mongoRule1 = new BasicDBObject();
        mongoRule1.put("_id", new ObjectId());
        mongoRule1.put("blacklist_id", mongoList.get("_id"));
        mongoRule1.put("term", "^foo.+");

        BasicDBObject mongoRule2 = new BasicDBObject();
        mongoRule2.put("_id", new ObjectId());
        mongoRule1.put("blacklist_id", mongoList.get("_id"));
        mongoRule2.put("term", ".+bar");

        BasicDBList rules = new BasicDBList();
        rules.add(mongoRule1);
        rules.add(mongoRule2);

        mongoList.put("blacklisted_terms", rules);

        Blacklist blacklist = new Blacklist(mongoList);

        assertEquals(2, blacklist.getRules().size());
        assertEquals("^foo.+", blacklist.getRules().get(0).getTerm());
        assertEquals(".+bar", blacklist.getRules().get(1).getTerm());
    }

    @Test
    public void testGetStreamRulesWithBlacklistThatHasNoRules() {
        BasicDBObject mongoList = this.buildMongoBlacklist();

        mongoList.put("blacklisted_terms", null);

        Blacklist blacklist = new Blacklist(mongoList);

        assertEquals(0, blacklist.getRules().size());
        
        // All is fine if there are no exceptions thrown and we get here.
    }

    @Test
    public void testGetId() {
        BasicDBObject mongo = this.buildMongoBlacklist();
        Blacklist list = new Blacklist(mongo);

        assertEquals(mongo.get("_id"), list.getId());
    }

    @Test
    public void testGetTitle() {
        Blacklist list = new Blacklist(this.buildMongoBlacklist());

        assertEquals("foo", list.getTitle());
    }

}