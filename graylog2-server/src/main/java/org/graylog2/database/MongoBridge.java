/**
 * Copyright 2010, 2011, 2012 Lennart Koopmann <lennart@socketfeed.com>
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

import java.util.Map;
import java.util.Set;

import org.graylog2.Core;
import org.graylog2.plugin.buffers.BufferWatermark;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;


/**
 * Simple mapping methods to MongoDB.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class MongoBridge {

    private static final Logger LOG = LoggerFactory.getLogger(MongoBridge.class);
    private MongoConnection connection;
    
    Core server;

    public MongoBridge(Core server) {
        this.server = server;
    }

    public MongoConnection getConnection() {
        return connection;
    }

    public void setConnection(MongoConnection connection) {
        this.connection = connection;
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

        DB db = getConnection().getDatabase();
        if (db == null) {
            // Not connected to DB.
            LOG.error("MongoBridge::upsertHost(): Could not get hosts collection.");
        } else {
            db.getCollection("hosts").update(query, update, true, false);
        }
    }

    public void writeThroughput(String serverId, int current, int highest) {
        BasicDBObject query = new BasicDBObject();
        query.put("server_id", serverId);
        query.put("type", "total_throughput");

        BasicDBObject update = new BasicDBObject();
        update.put("server_id", serverId);
        update.put("type", "total_throughput");
        update.put("current", current);
        update.put("highest", highest);

        DBCollection coll = getConnection().getDatabase().getCollection("server_values");
        coll.update(query, update, true, false);
    }
    
    public void writeBufferWatermarks(String serverId, BufferWatermark outputBuffer, BufferWatermark processBuffer) {
        BasicDBObject query = new BasicDBObject();
        query.put("server_id", serverId);
        query.put("type", "buffer_watermarks");

        BasicDBObject update = new BasicDBObject();
        update.put("server_id", serverId);
        update.put("type", "buffer_watermarks");
        
        update.put("outputbuffer", outputBuffer.getUtilization());
        update.put("outputbuffer_percent", outputBuffer.getUtilizationPercentage());

        update.put("processbuffer", processBuffer.getUtilization());
        update.put("processbuffer_percent", processBuffer.getUtilizationPercentage());
        
        DBCollection coll = getConnection().getDatabase().getCollection("server_values");
        coll.update(query, update, true, false);
    }
    
    public void writeMasterCacheSizes(String serverId, int inputCacheSize, int outputCacheSize) {
        BasicDBObject query = new BasicDBObject();
        query.put("server_id", serverId);
        query.put("type", "mastercache_sizes");

        BasicDBObject update = new BasicDBObject();
        update.put("server_id", serverId);
        update.put("type", "mastercache_sizes");
        
        update.put("inputcache", inputCacheSize);
        update.put("outputcache", outputCacheSize);
        
        DBCollection coll = getConnection().getDatabase().getCollection("server_values");
        coll.update(query, update, true, false);
    }

    public void setSimpleServerValue(String serverId, String key, Object value) {
        BasicDBObject query = new BasicDBObject();
        query.put("server_id", serverId);
        query.put("type", key);

        BasicDBObject update = new BasicDBObject();
        update.put("server_id", serverId);
        update.put("value", value);
        update.put("type", key);

        MongoConnection connection2 = getConnection();
        DB database = connection2.getDatabase();
        DBCollection coll = database.getCollection("server_values");
        coll.update(query, update, true, false);
    }

    public void writeMessageCounts(int total, Map<String, Integer> streams, Map<String, Integer> hosts) {
        // We store the first second of the current minute, to allow syncing (summing) message counts
        // from different graylog-server nodes later
        DateTime dt = new DateTime();
        int startOfMinute = Tools.getUTCTimestamp()-dt.getSecondOfMinute();;
        
        BasicDBObject obj = new BasicDBObject();
        obj.put("timestamp", startOfMinute);
        obj.put("total", total);
        obj.put("streams", streams);
        obj.put("hosts", hosts);
        obj.put("server_id", server.getNodeId());

        getConnection().getMessageCountsColl().insert(obj);
    }
    
    public void writeDeflectorInformation(Map<String, Object> info) {
        DBCollection coll = connection.getDatabase().getCollection("deflector_informations");

        // Delete all entries, we only have one at a time.
        coll.remove(new BasicDBObject());
        
        BasicDBObject obj = new BasicDBObject(info);
        coll.insert(obj);
    }
    
    public void writePluginInformation(Set<Map<String, Object>> plugins, String collection) {
        DBCollection coll = connection.getDatabase().getCollection(collection);

        // Delete all entries, we only have one at a time.
        coll.remove(new BasicDBObject());
        
        for (Map<String, Object> plugin : plugins) {
            writeSinglePluginInformation(plugin, collection);
        }
    }
    
    public void writeSinglePluginInformation(Map<String, Object> plugin, String collection) {
        DBCollection coll = connection.getDatabase().getCollection(collection);

        DBObject query = new BasicDBObject();
        query.put("typeclass", plugin.get("typeclass"));
        
        // Upsert, because there might be a plugin already and we don't purge for single.
        coll.update(query, new BasicDBObject(plugin), true, false);
    }
    
    /**
     * Get a setting from the settings collection.
     *
     * @param type The TYPE (See constants in Setting class) to fetch.
     * @return The settings - Can be null.
     */
    public DBObject getSetting(int type) {
        DBCollection coll = getConnection().getDatabase().getCollection("settings");

        DBObject query = new BasicDBObject();
        query.put("setting_type", type);
        return coll.findOne(query);
    }


}
