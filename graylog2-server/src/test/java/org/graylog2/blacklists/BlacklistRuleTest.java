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

import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.*;

public class BlacklistRuleTest {

    @Test
    public void testGetTerm() {
        BasicDBObject mongo = new BasicDBObject();
        ObjectId id = new ObjectId();
        String term = "^foo.+";
        
        mongo.put("_id", id);
        mongo.put("term", term);

        BlacklistRule rule = new BlacklistRule(mongo);

        assertEquals(id, rule.getId());
        assertEquals(term, rule.getTerm());
    }

}