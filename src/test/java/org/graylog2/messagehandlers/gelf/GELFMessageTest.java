/**
 * Copyright 2010, 2011 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.messagehandlers.gelf;

import com.mongodb.BasicDBList;
import org.bson.types.ObjectId;
import com.mongodb.BasicDBObject;
import java.util.ArrayList;
import java.util.List;
import org.graylog2.blacklists.Blacklist;
import org.junit.Test;
import static org.junit.Assert.*;

public class GELFMessageTest {

    @Test
    public void testBlacklistedWithFalseResult() {
        BasicDBObject mongoList = new BasicDBObject();
        mongoList.put("_id", new ObjectId());
        mongoList.put("title", "foo");

        BasicDBObject mongoRule1 = new BasicDBObject();
        mongoRule1.put("_id", new ObjectId());
        mongoRule1.put("blacklist_id", mongoList.get("_id"));
        mongoRule1.put("term", "^foo.+");

        BasicDBObject mongoRule2 = new BasicDBObject();
        mongoRule2.put("_id", new ObjectId());
        mongoRule1.put("blacklist_id", mongoList.get("_id"));
        mongoRule2.put("term", ".+aarrghhhllll");

        BasicDBList rules = new BasicDBList();
        rules.add(mongoRule1);
        rules.add(mongoRule2);

        mongoList.put("blacklisted_terms", rules);

        Blacklist blacklist = new Blacklist(mongoList);

        GELFMessage msg = new GELFMessage();
        msg.setShortMessage("ohai thar foo");

        List<Blacklist> blacklists = new ArrayList<Blacklist>();
        blacklists.add(blacklist);

        assertFalse(msg.blacklisted(blacklists));
    }

    @Test
    public void testBlacklistedWithPositiveResult() {
        BasicDBObject mongoList = new BasicDBObject();
        mongoList.put("_id", new ObjectId());
        mongoList.put("title", "foo");

        BasicDBObject mongoRule1 = new BasicDBObject();
        mongoRule1.put("_id", new ObjectId());
        mongoRule1.put("blacklist_id", mongoList.get("_id"));
        mongoRule1.put("term", "^ohai.+");

        BasicDBObject mongoRule2 = new BasicDBObject();
        mongoRule2.put("_id", new ObjectId());
        mongoRule1.put("blacklist_id", mongoList.get("_id"));
        mongoRule2.put("term", ".+aarrghhhllll");

        BasicDBList rules = new BasicDBList();
        rules.add(mongoRule1);
        rules.add(mongoRule2);

        mongoList.put("blacklisted_terms", rules);

        Blacklist blacklist = new Blacklist(mongoList);

        GELFMessage msg = new GELFMessage();
        msg.setShortMessage("ohai thar foo");

        List<Blacklist> blacklists = new ArrayList<Blacklist>();
        blacklists.add(blacklist);

        assertTrue(msg.blacklisted(blacklists));
    }

}