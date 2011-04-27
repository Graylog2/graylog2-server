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

package org.graylog2.hostgroups;

import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.junit.Test;
import static org.junit.Assert.*;


public class HostgroupTest {

    @Test
    public void testStream() {
        BasicDBObject mongo = new BasicDBObject();

        ObjectId id = new ObjectId();
        mongo.put("_id", id);
        mongo.put("name", "foo");

        Hostgroup group = new Hostgroup(mongo);

        assertEquals(id, group.getId());
        assertEquals("foo", group.getName());
    }

    // Get hosts not really testable because it is not an embedded doc of hostgroup yet.

}