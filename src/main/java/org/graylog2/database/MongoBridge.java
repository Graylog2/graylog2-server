/**
 * Copyright 2010 Lennart Koopmann <lennart@scopeport.org>
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

/**
 * MongoBridge.java: lennart | Apr 13, 2010 9:13:03 PM
 */

package org.graylog2.database;

import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;

import org.graylog2.Log;

import java.util.Iterator;
import java.util.List;
import org.graylog2.Main;
import org.graylog2.messagehandlers.gelf.GELFMessage;

import org.productivity.java.syslog4j.server.SyslogServerEventIF;

public class MongoBridge {
    // TODO: make configurable
    public static final int STANDARD_PORT = 27017;

    public DBCollection getMessagesColl() {
        DBCollection coll = null;

        // Create a capped collection if the collection does not yet exist.
        if(MongoConnection.getInstance().getDatabase().getCollectionNames().contains("messages")) {
            coll = MongoConnection.getInstance().getDatabase().getCollection("messages");
        } else {
            int messagesCollSize = Integer.parseInt(Main.masterConfig.getProperty("messages_collection_size").trim());
            coll = MongoConnection.getInstance().getDatabase().createCollection("messages", BasicDBObjectBuilder.start().add("capped", true).add("size", messagesCollSize).get());
        }

        coll.ensureIndex(new BasicDBObject("created_at", 1));
        coll.ensureIndex(new BasicDBObject("host", 1));
        coll.ensureIndex(new BasicDBObject("facility", 1));
        coll.ensureIndex(new BasicDBObject("level", 1));

        return coll;
    }

    public void insert(SyslogServerEventIF event) throws Exception {
        DBCollection coll = this.getMessagesColl();

        BasicDBObject dbObj = new BasicDBObject();
        dbObj.put("message", event.getMessage());
        dbObj.put("host", event.getHost());
        dbObj.put("facility", event.getFacility());
        dbObj.put("level", event.getLevel());
        dbObj.put("created_at", (int) (System.currentTimeMillis()/1000));

        coll.insert(dbObj);
    }

    public void insertGelfMessage(GELFMessage message) throws Exception {
        // Check if all required parameters are set.
        if (message.shortMessage == null || message.shortMessage.length() == 0 || message.host == null || message.host.length() == 0) {
            throw new Exception("Missing GELF message parameters. short_message and host are required.");
        }
        DBCollection coll = this.getMessagesColl();

        BasicDBObject dbObj = new BasicDBObject();

        dbObj.put("gelf", true);
        dbObj.put("message", message.shortMessage);
        dbObj.put("full_message", message.fullMessage);
        dbObj.put("type", message.type);
        dbObj.put("file", message.file);
        dbObj.put("line", message.line);
        dbObj.put("host", message.host);
        dbObj.put("facility", null);
        dbObj.put("level", message.level);
        dbObj.put("created_at", (int) (System.currentTimeMillis()/1000));

        coll.insert(dbObj);
    }

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
        for (Iterator<String> i = hosts.iterator(); i.hasNext( ); ) {
            try {
                String host = i.next();

                // Skip hosts with no name.
                if (host != null && host.length() > 0) {
                    // Get message count of this host.
                    BasicDBObject countQuery = new BasicDBObject();
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
                continue;
            }
        }
    }

}
