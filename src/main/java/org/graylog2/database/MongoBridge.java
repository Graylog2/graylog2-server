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

package org.graylog2.database;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.apache.log4j.Logger;
import org.graylog2.Tools;

import java.util.Map;

/**
 * MongoBridge.java: Apr 13, 2010 9:13:03 PM
 *
 * Simple mapping methods to MongoDB.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class MongoBridge {

    private static final Logger LOG = Logger.getLogger(MongoBridge.class);

    /**
     * Adds x to the counter of host in "hosts" collection.
     *
     * @param hostname The host to increment.
     * @param add The value to add to the current counter value.
     */
    public void upsertHostCount(String hostname, int add) {
        BasicDBObject query = new BasicDBObject();
        query.put("host", hostname);

        BasicDBObject update = new BasicDBObject();
        update.put("$inc", new BasicDBObject("message_count", add));

        DB db = MongoConnection.getInstance().getDatabase();
        if (db == null) {
            // Not connected to DB.
            LOG.error("MongoBridge::upsertHost(): Could not get hosts collection.");
        } else {
            db.getCollection("hosts").update(query, update, true, false);
        }
    }

    public void writeThroughput(int current, int highest) {
        BasicDBObject query = new BasicDBObject();
        query.put("type", "total_throughput");
        
        BasicDBObject update = new BasicDBObject();
        update.put("type", "total_throughput");
        update.put("current", current);
        update.put("highest", highest);
        
        DBCollection coll = MongoConnection.getInstance().getDatabase().getCollection("server_values");
        coll.update(query, update, true, false);
    }

    public void setSimpleServerValue(String key, Object value) {
        BasicDBObject query = new BasicDBObject();
        query.put("type", key);

        BasicDBObject update = new BasicDBObject();
        update.put("value", value);
        update.put("type", key);

        DBCollection coll = MongoConnection.getInstance().getDatabase().getCollection("server_values");
        coll.update(query, update, true, false);
    }

    public void writeMessageCounts(int total, Map<String, Integer> streams, Map<String, Integer> hosts) {
        BasicDBObject obj = new BasicDBObject();
        obj.put("timestamp", Tools.getUTCTimestamp());
        obj.put("total", total);
        obj.put("streams", streams);
        obj.put("hosts", hosts);

        MongoConnection.getInstance().getMessageCountsColl().insert(obj);
    }

    /**
     * Get a setting from the settings collection.
     *
     * @param type The TYPE (See constants in Setting class) to fetch.
     * @return The settings - Can be null.
     */
    public DBObject getSetting(int type) {
        DBCollection coll = MongoConnection.getInstance().getDatabase().getCollection("settings");

        DBObject query = new BasicDBObject();
        query.put("setting_type", type);
        return coll.findOne(query);
    }

}
