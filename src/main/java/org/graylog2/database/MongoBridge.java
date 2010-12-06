/**
 * Copyright 2010 Lennart Koopmann <lennart@socketfeed.com>
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

import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import java.util.Iterator;

import org.graylog2.Log;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.graylog2.Main;
import org.graylog2.messagehandlers.gelf.GELFMessage;

import org.productivity.java.syslog4j.server.SyslogServerEventIF;

/**
 * MongoBridge.java: Apr 13, 2010 9:13:03 PM
 *
 * Simple mapping methods to MongoDB.
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class MongoBridge {

    /**
     * The standard MongoDB port.
     */
    public static final int STANDARD_PORT = 27017;

    /**
     * Get the messages collection. Lazily creates a new, capped one based on the
     * messages_collection_size from graylog2.conf if there is none.
     *
     * @return The messages collection
     */
    public DBCollection getMessagesColl() {
        DBCollection coll = null;

        // Create a capped collection if the collection does not yet exist.
        if(MongoConnection.getInstance().getDatabase().getCollectionNames().contains("messages")) {
            coll = MongoConnection.getInstance().getDatabase().getCollection("messages");
        } else {
            int messagesCollSize = Integer.parseInt(Main.masterConfig.getProperty("messages_collection_size").trim());
            coll = MongoConnection.getInstance().getDatabase().createCollection("messages", BasicDBObjectBuilder.start().add("capped", true).add("size", messagesCollSize).get());
        }

        // XXX PERFORMANCE
        coll.ensureIndex(new BasicDBObject("created_at", 1));
        coll.ensureIndex(new BasicDBObject("host", 1));
        coll.ensureIndex(new BasicDBObject("facility", 1));
        coll.ensureIndex(new BasicDBObject("level", 1));

        return coll;
    }

    /**
     * Inserts a Syslog message into the messages collection.
     *
     * @param event The syslog event/message
     * @throws Exception
     */
    public void insert(SyslogServerEventIF event) throws Exception {
        DBCollection coll = this.getMessagesColl();

        BasicDBObject dbObj = new BasicDBObject();
        dbObj.put("message", event.getMessage());
        dbObj.put("host", event.getHost());
        dbObj.put("facility", event.getFacility());
        dbObj.put("level", event.getLevel());
        dbObj.put("created_at", (int) (System.currentTimeMillis()/1000));
        // Documents in capped collections cannot grow so we have to do that now and cannot just add 'deleted => true' later.
        dbObj.put("deleted", false);

        coll.insert(dbObj);
    }

    /**
     * Inserts a GELF message into the messages collection.
     *
     * @param message The GELF message
     * @throws Exception
     */
    public void insertGelfMessage(GELFMessage message) throws Exception {
        // Check if all required parameters are set.
        if (message.getShortMessage() == null || message.getHost() == null  || message.getShortMessage().length() == 0 || message.getHost().length() == 0) {
            throw new Exception("Missing GELF message parameters. short_message and host are required.");
        }
        DBCollection coll = this.getMessagesColl();

        BasicDBObject dbObj = new BasicDBObject();

        dbObj.put("gelf", true);
        dbObj.put("message", message.getShortMessage());
        dbObj.put("full_message", message.getFullMessage());
        dbObj.put("file", message.getFile());
        dbObj.put("line", message.getLine());
        dbObj.put("host", message.getHost());
        dbObj.put("facility", null);
        dbObj.put("level", message.getLevel());

        // Add additional fields. XXX PERFORMANCE
        Map<String,String> additionalFields = message.getAdditionalData();
        Set<String> set = additionalFields.keySet();
        Iterator<String> iter = set.iterator();
        while(iter.hasNext()) {
            String key = iter.next();
            String value = additionalFields.get(key);
            dbObj.put(key, value);
        }

        dbObj.put("created_at", (int) (System.currentTimeMillis()/1000));
        // Documents in capped collections cannot grow so we have to do that now and cannot just add 'deleted => true' later.
        dbObj.put("deleted", false);

        coll.insert(dbObj);
    }

    /**
     * Builds the "host" collection by distincting all hosts
     * from the messages table.
     *
     * @throws Exception
     */
    public void distinctHosts() throws Exception {
        // Fetch all hosts.
        DBCollection messages = this.getMessagesColl();
        List<String> hosts = messages.distinct("host");

        DBCollection coll = null;

        // Create a capped collection if the collection does not yet exist.
        if(MongoConnection.getInstance().getDatabase().getCollectionNames().contains("hosts")) {
            coll = MongoConnection.getInstance().getDatabase().getCollection("hosts");
        } else {
            coll = MongoConnection.getInstance().getDatabase().createCollection("hosts", new BasicDBObject());
        }

        coll.ensureIndex(new BasicDBObject("name", 1));

        // Truncate host collection.
        coll.remove(new BasicDBObject());
        
        // Go trough every host and insert.
        for (String host : hosts) {
            try {
                // Skip hosts with no name.
                if (host != null && host.length() > 0) {
                    // Get message count of this host.
                    BasicDBObject countQuery = new BasicDBObject();
                    countQuery.put("deleted", false);
                    countQuery.put("host", host);
                    long messageCount = messages.getCount(countQuery);

                    // Build document.
                    BasicDBObject doc = new BasicDBObject();
                    doc.put("host", host);
                    doc.put("message_count", messageCount);

                    // Store document.
                    coll.insert(doc);
                }
            } catch (Exception e) {
                Log.crit("Could not insert distinct host: " + e.toString());
                e.printStackTrace();
            }
        }
    }

    public void writeGraphInformation(String host, int value) {
        DBCollection coll = MongoConnection.getInstance().getDatabase().getCollection("graphs");

        BasicDBObject obj = new BasicDBObject();
        obj.put("host", host);
        obj.put("value", value);
        obj.put("created_at", (int) (System.currentTimeMillis()/1000));

        coll.insert(obj);
    }
}
