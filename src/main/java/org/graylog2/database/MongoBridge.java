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
import com.mongodb.WriteConcern;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.graylog2.Main;
import org.graylog2.Tools;
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
        dbObj.put("created_at", Tools.getUTCTimestamp());
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
        if (!message.allRequiredFieldsSet()) {
            throw new Exception("Missing GELF message parameters. _version, _short_message and _host are required.");
        }

        DBCollection coll = this.getMessagesColl();

        BasicDBObject dbObj = new BasicDBObject();

        dbObj.put("gelf", true);
        dbObj.put("_version", message.getVersion());
        dbObj.put("_message", message.getShortMessage());
        dbObj.put("_full_message", message.getFullMessage());
        dbObj.put("_file", message.getFile());
        dbObj.put("_line", message.getLine());
        dbObj.put("_host", message.getHost());
        dbObj.put("_facility", message.getFacility()); 
        dbObj.put("_level", message.getLevel());
        dbObj.put("_timestamp", message.getTimestamp());

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

        coll.insert(dbObj);
    }

    /**
     * Atomically increments counter of host in "hosts" collection by one.
     *
     * @param hostname The host to increment.
     */
    public void upsertHost(String hostname) {
        BasicDBObject query = new BasicDBObject();
        query.put("host", hostname);

        BasicDBObject update = new BasicDBObject();
        update.put("$inc", new BasicDBObject("message_count", 1));

        DBCollection coll = MongoConnection.getInstance().getDatabase().getCollection("hosts");
        coll.update(query, update, true, false);
    }

}
