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
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoConnection;

/**
 * Hostgroup.java: Apr 15, 2011 11:50:46 AM
 *
 * Representing a single hostgroup from the hostgroups collection. Also provides method
 * to get all groups out of this collection.
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class Hostgroup {

    private static final Logger LOG = Logger.getLogger(Hostgroup.class);

    private ObjectId id = null;
    private String name = null;

    private List<HostgroupHost> hosts = null;

    private DBObject mongoObject = null;

    public Hostgroup (DBObject group) {
        this.id = (ObjectId) group.get("_id");
        this.name = (String) group.get("name");
        this.mongoObject = group;
    }

    public static ArrayList<Hostgroup> fetchAll() {
        if (HostgroupCache.getInstance().valid()) {
            return HostgroupCache.getInstance().get();
        }

        ArrayList<Hostgroup> groups = new ArrayList<Hostgroup>();

        DBCollection coll = MongoConnection.getInstance().getDatabase().getCollection("hostgroups");
        DBCursor cur = coll.find(new BasicDBObject());

        while (cur.hasNext()) {
            try {
                groups.add(new Hostgroup(cur.next()));
            } catch (Exception e) {
                LOG.warn("Can't fetch host group. Skipping. " + e.getMessage(), e);
            }
        }

        HostgroupCache.getInstance().set(groups);

        return groups;
    }

    /**
     * @return the id
     */
    public ObjectId getId() {
        return id;
    }

    /**
     * @return the title
     */
    public String getName() {
        return name;
    }

}