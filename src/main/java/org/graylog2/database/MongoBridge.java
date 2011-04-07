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
import org.apache.log4j.Logger;
import org.graylog2.Tools;
import org.graylog2.messagehandlers.gelf.GELFMessage;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * MongoBridge.java: Apr 13, 2010 9:13:03 PM
 *
 * Simple mapping methods to MongoDB.
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class MongoBridge {

    private static final Logger LOG = Logger.getLogger(MongoBridge.class);

    /**
     * Inserts a GELF message into the messages collection.
     *
     * @param message The GELF message
     * @throws Exception
     */
    public void insertGelfMessage(GELFMessage message) throws Exception {
        // Check if all required parameters are set.
        if (!message.allRequiredFieldsSet()) {
            throw new Exception("Missing GELF message parameters. version, short_message and host are required.");
        }

        DBCollection coll = MongoConnection.getInstance().getMessagesColl();

        BasicDBObject dbObj = new BasicDBObject();

        // Some fields must not be set if this message was converted from a syslog message.
        if (!message.convertedFromSyslog()) {
            dbObj.put("gelf", true);
            dbObj.put("version", message.getVersion());
        }

        dbObj.put("message", message.getShortMessage());
        dbObj.put("full_message", message.getFullMessage());
        dbObj.put("file", message.getFile());
        dbObj.put("line", message.getLine());
        dbObj.put("host", message.getHost());
        dbObj.put("facility", message.getFacility()); 
        dbObj.put("level", message.getLevel());
        dbObj.put("timestamp", message.getTimestamp());
        
        // Add additional fields. XXX PERFORMANCE
        Map<String,String> additionalFields = message.getAdditionalData();
        Set<String> set = additionalFields.keySet();
        Iterator<String> iter = set.iterator();
        while(iter.hasNext()) {
            String key = iter.next();
            String value = additionalFields.get(key);
            dbObj.put(key, value);
        }

        dbObj.put("created_at", Tools.getUTCTimestamp());
        // Documents in capped collections cannot grow so we have to do that now and cannot just add 'deleted => true' later.
        dbObj.put("deleted", false);

        dbObj.put("streams", message.getStreamIds());

        coll.insert(dbObj);
    }

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

    public void writeHistoricServerValue(String key, Object value) {
        BasicDBObject obj = new BasicDBObject();
        obj.put("type", key);
        obj.put("value", value);
        obj.put("created_at", Tools.getUTCTimestamp());

        DBCollection coll = MongoConnection.getInstance().getHistoricServerValuesColl();
        coll.insert(obj);
    }

}
