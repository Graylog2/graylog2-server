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

import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.graylog2.Log;
import org.graylog2.Main;
import org.graylog2.Tools;
import org.graylog2.messagehandlers.gelf.GELFMessage;
import org.graylog2.messagehandlers.syslog.GraylogSyslogServerEvent;

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
        if(MongoConnection.getInstance().getDatabase().collectionExists("messages")) {
            coll = MongoConnection.getInstance().getDatabase().getCollection("messages");
        } else {
            long messagesCollSize = Long.parseLong(Main.masterConfig.getProperty("messages_collection_size").trim());
            coll = MongoConnection.getInstance()
                    .getDatabase()
                    .createCollection("messages", BasicDBObjectBuilder.start()
                    .add("capped", true)
                    .add("size", messagesCollSize)
                    .get());
        }

        coll.ensureIndex(new BasicDBObject("created_at", 1));
        coll.ensureIndex(new BasicDBObject("deleted", 1));
        coll.ensureIndex(new BasicDBObject("host", 1));
        coll.ensureIndex(new BasicDBObject("facility", 1));
        coll.ensureIndex(new BasicDBObject("level", 1));

        return coll;
    }

    public DBCollection getHistoricServerValuesColl() {
        DBCollection coll = null;

        // Create a capped collection if the collection does not yet exist.
        if(MongoConnection.getInstance().getDatabase().getCollectionNames().contains("historic_server_values")) {
            coll = MongoConnection.getInstance().getDatabase().getCollection("historic_server_values");
        } else {
            coll = MongoConnection.getInstance()
                    .getDatabase().createCollection("historic_server_values", BasicDBObjectBuilder.start()
                    .add("capped", true)
                    .add("size", 10000000) // 10 MB
                    .get());
        }

        coll.ensureIndex(new BasicDBObject("type", 1));
        coll.ensureIndex(new BasicDBObject("created_at", 1));

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

        // Add AMQP receiver queue if this is an extended event.
        if (event instanceof GraylogSyslogServerEvent) {
            GraylogSyslogServerEvent extendedEvent = (GraylogSyslogServerEvent) event;
            dbObj.put("_amqp_queue", extendedEvent.getAmqpReceiverQueue());
        }

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
            throw new Exception("Missing GELF message parameters. version, short_message and host are required.");
        }

        DBCollection coll = this.getMessagesColl();

        BasicDBObject dbObj = new BasicDBObject();

        dbObj.put("gelf", true);
        dbObj.put("version", message.getVersion());
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
            Log.emerg("MongoBridge::upsertHost(): Could not get hosts collection.");
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

        DBCollection coll = getHistoricServerValuesColl();
        coll.insert(obj);
    }

}
