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

public class FileNameAndLineMatcherTest {
/*
    @Test
    public void testSuccessfulMath() {
        String filename = "main.rb";
        int line = 17;
        String regex = "^main\\.rb:1\\d";

        BasicDBObject mongoRule = new BasicDBObject();
        mongoRule.put("_id", new ObjectId());
        mongoRule.put("rule_type", StreamRule.TYPE_FILENAME_LINE);
        mongoRule.put("value",  regex);

        StreamRule rule = new StreamRule(mongoRule);
        FileNameAndLineMatcher matcher = new FileNameAndLineMatcher();
        GELFMessage msg = new GELFMessage();
        
        msg.setFile(filename);
        msg.setLine(17);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testSuccessfulMatchWithoutRegex() {
        String filename = "lol.php";
        int line = 9001;
        String regex = "lol\\.php:9001";

        BasicDBObject mongoRule = new BasicDBObject();
        mongoRule.put("_id", new ObjectId());
        mongoRule.put("rule_type", StreamRule.TYPE_FILENAME_LINE);
        mongoRule.put("value",  regex);

        StreamRule rule = new StreamRule(mongoRule);
        FileNameAndLineMatcher matcher = new FileNameAndLineMatcher();
        GELFMessage msg = new GELFMessage();

        msg.setFile(filename);
        msg.setLine(9001);

        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testSuccessfulMatchWithoutLineNumber() {
        String filename = "lol.php";
        String regex = "^lol\\.php";

        BasicDBObject mongoRule = new BasicDBObject();
        mongoRule.put("_id", new ObjectId());
        mongoRule.put("rule_type", StreamRule.TYPE_FILENAME_LINE);
        mongoRule.put("value",  regex);

        StreamRule rule = new StreamRule(mongoRule);
        FileNameAndLineMatcher matcher = new FileNameAndLineMatcher();
        GELFMessage msg = new GELFMessage();

        msg.setFile(filename);
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testMissedMatch() {
        String filename = "main.rb";
        int line = 27;
        String regex = "^main\\.rb:1\\d";

        BasicDBObject mongoRule = new BasicDBObject();
        mongoRule.put("_id", new ObjectId());
        mongoRule.put("rule_type", StreamRule.TYPE_FILENAME_LINE);
        mongoRule.put("value",  regex);

        StreamRule rule = new StreamRule(mongoRule);
        FileNameAndLineMatcher matcher = new FileNameAndLineMatcher();
        GELFMessage msg = new GELFMessage();

        msg.setFile(filename);
        msg.setLine(line);
        assertFalse(matcher.match(msg, rule));
    }
*/
}