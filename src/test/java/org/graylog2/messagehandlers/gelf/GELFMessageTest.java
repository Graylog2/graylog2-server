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
import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.graylog2.blacklists.Blacklist;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

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

    @Test
    public void testBlacklistedWithPositiveResultAndNewline() {
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
        msg.setShortMessage("ohai thar\nfoo");

        List<Blacklist> blacklists = new ArrayList<Blacklist>();
        blacklists.add(blacklist);

        assertTrue(msg.blacklisted(blacklists));
    }

    @Test
    public void testToOneliner() {

        GELFMessage gelfMessage = createGELFMessage();

        String oneLiner = "host.example.com - short message severity=Emergency,facility=local0,file=test.file,line=42,test=test";
        assertEquals(oneLiner, gelfMessage.toOneLiner());
    }

    private GELFMessage createGELFMessage() {

        GELFMessage gelfMessage = new GELFMessage();

        gelfMessage.setHost("host.example.com");
        gelfMessage.setShortMessage("short message");
        gelfMessage.setFullMessage("full message");
        gelfMessage.setVersion("1");
        gelfMessage.setLevel(0);
        gelfMessage.setFacility("local0");
        gelfMessage.setFile("test.file");
        gelfMessage.setLine(42);
        gelfMessage.addAdditionalData("test", "test");

        return gelfMessage;
    }

    @Test
    public void testToString() {

        String stringDelim = " | ";

        GELFMessage gelfMessage = createGELFMessage();

        String toString = "shortMessage: short message" + stringDelim;
        toString += "fullMessage: full message" + stringDelim;
        toString += "level: 0" + stringDelim;
        toString += "host: host.example.com" + stringDelim;
        toString += "file: test.file" + stringDelim;
        toString += "line: 42" + stringDelim;
        toString += "facility: local0" + stringDelim;
        toString += "version: 1" + stringDelim;
        toString += "additional: 1";

        assertEquals(toString, gelfMessage.toString());
    }

    @Test
    public void testToStringWithLongMessage() {

        GELFMessage gelfMessage = createGELFMessage();
        gelfMessage.setFullMessage("Really, really, really, really, really, really, really, really, really, really, "
                + "really, really, really, really, really, really, really, really, really, really, really, really, "
                + "really, really, really, really, really, really, really, really, really, really, really, really, "
                + "really, really long");

        String toString = "shortMessage: short message | fullMessage: Really, really, really, really, really, really, "
                + "really, really, really, really, really, really, really, really, really, really, really, really, "
                + "really, really, really, really, really (...)";

        assertEquals(toString, gelfMessage.toString());
    }

    @Test
    public void testAllRequiredFieldsSet() {

        GELFMessage emptyGelfMessage = new GELFMessage();
        assertFalse(emptyGelfMessage.allRequiredFieldsSet());

        GELFMessage versionMissingGelfMessage = createGELFMessage();
        versionMissingGelfMessage.setVersion("");
        assertFalse(versionMissingGelfMessage.allRequiredFieldsSet());

        GELFMessage gelfMessage = createGELFMessage();
        assertTrue(gelfMessage.allRequiredFieldsSet());
    }
}