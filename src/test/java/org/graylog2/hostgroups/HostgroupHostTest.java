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
import com.mongodb.DBObject;
import org.bson.types.ObjectId;

import org.junit.Test;
import static org.junit.Assert.*;

public class HostgroupHostTest {

    public DBObject buildMongoHostgroupHost() {
        DBObject m = new BasicDBObject();
        m.put("hostgroup_id", new ObjectId());
        m.put("ruletype", HostgroupHost.TYPE_SIMPLE);
        m.put("hostname", "example.org");

        return m;
    }

    /**
     * Test of getHostgroupId method, of class HostgroupHost.
     */
    @Test
    public void testGetHostgroupId() {
        DBObject m = this.buildMongoHostgroupHost();
        HostgroupHost hh = new HostgroupHost(m);
        assertEquals(m.get("hostgroup_id"), hh.getHostgroupId());
    }

    /**
     * Test of getType method, of class HostgroupHost.
     */
    @Test
    public void testGetType() {
        DBObject m = this.buildMongoHostgroupHost();
        HostgroupHost hh = new HostgroupHost(m);
        assertEquals(m.get("ruletype"), hh.getType());
    }

    /**
     * Test of getHostname method, of class HostgroupHost.
     */
    @Test
    public void testGetHostname() {
        DBObject m = this.buildMongoHostgroupHost();
        HostgroupHost hh = new HostgroupHost(m);
        assertEquals(m.get("hostname"), hh.getHostname());
    }

}