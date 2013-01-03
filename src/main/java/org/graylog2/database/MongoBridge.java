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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.graylog2.Core;
import org.graylog2.activities.Activity;
import org.graylog2.buffers.BufferWatermark;
import org.graylog2.plugin.Counter;
import org.graylog2.plugin.MessageCounter;
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

    public void writeThroughput(String serverId, int current) {
        BasicDBObject totalQuery = new BasicDBObject();
        totalQuery.put("server_id", serverId);
        totalQuery.put("type", "total_throughput");

        BasicDBObject totalUpdate = new BasicDBObject();
        totalUpdate.put("$set", new BasicDBObject("current", current));

        BasicDBObject highestQuery = new BasicDBObject();
        highestQuery.put("server_id", serverId);
        highestQuery.put("type", "total_throughput");
        highestQuery.put("highest", new BasicDBObject("$lt", current));

        BasicDBObject highestUpdate = new BasicDBObject();
        highestUpdate.put("$set", new BasicDBObject("highest", current));

        DBCollection coll = getConnection().getDatabase().getCollection("server_values");
        coll.update(totalQuery, totalUpdate, true, false);
        coll.update(highestQuery, highestUpdate, true, false);
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

    public void writeMessageCounts(Counter total, Map<String, Counter> streams, Map<String, Counter> hosts) {
        // We store the first second of the current minute, to allow syncing (summing) message counts
        // from different graylog-server nodes later
        DateTime dt = new DateTime();
        int startOfMinute = Tools.getUTCTimestamp()-dt.getSecondOfMinute();;

        BasicDBObject obj = new BasicDBObject();
        obj.put("timestamp", startOfMinute);
        obj.put("total", total.get());
        obj.put("streams", streams);
        obj.put("hosts", hosts);
        obj.put("server_id", server.getServerId());

        getConnection().getMessageCountsColl().insert(obj);
    }

    public void writeMessageCounts(int timestamp, MessageCounter counter) {
        BasicDBObject queryObject = new BasicDBObject();
        queryObject.put("timestamp", timestamp);
        queryObject.put("server_id", server.getServerId());

        //TODO check streams and hosts
        BasicDBObject incObject = new BasicDBObject();
        incObject.put("total", counter.getTotalCount());
        incObject.put("streams", counter.getStreamCounts());
        incObject.put("hosts", counter.getHostCounts());

        BasicDBObject updateObject = new BasicDBObject();
        updateObject.put("$inc", incObject);

        getConnection().getMessageCountsColl().update(queryObject, updateObject, true, false);
    }

    public void writeActivity(Activity activity, String nodeId) {
        BasicDBObject obj = new BasicDBObject();
        obj.put("timestamp", Tools.getUTCTimestamp());
        obj.put("content", activity.getMessage());
        obj.put("caller", activity.getCaller().getCanonicalName());
        obj.put("node_id", nodeId);

        connection.getDatabase().getCollection("server_activities").insert(obj);
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

    public void writeIndexDateRange(String indexName, int startDate) {
        BasicDBObject obj = new BasicDBObject();
        obj.put("index", indexName);
        obj.put("start", startDate);

        connection.getDatabase().getCollection("index_ranges").insert(obj);
    }

    public List<DBObject> getIndexDateRanges() {
        return connection.getDatabase().getCollection("index_ranges").find().toArray();
    }

    public void removeIndexDateRange(String indexName) {
        BasicDBObject obj = new BasicDBObject();
        obj.put("index", indexName);

        connection.getDatabase().getCollection("index_ranges").remove(obj);
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
