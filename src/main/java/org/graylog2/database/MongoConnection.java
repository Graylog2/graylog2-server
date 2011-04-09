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
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.Mongo;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoException;
import com.mongodb.MongoOptions;
import com.mongodb.ServerAddress;
import java.util.List;
import org.graylog2.Configuration;
import org.graylog2.Main;

/**
 * MongoConnection.java: Jun 6, 2010 1:36:19 PM
 *
 * MongoDB connection singleton
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public final class MongoConnection {
    private static MongoConnection instance;

    private Mongo m = null;
    private DB db = null;

    private DBCollection messagesCollection = null;
    private DBCollection historicServerValuesCollection = null;

    private MongoConnection() {}

    /**
     * Get the connection instance
     * @return MongoConnection instance
     */
    public synchronized static MongoConnection getInstance() {
        if (instance == null) {
            instance = new MongoConnection();
        }
        return instance;
    }

    /**
     * Connect the instance.
     *
     * @param username MongoDB user
     * @param password MongoDB password
     * @param hostname MongoDB host
     * @param database MongoDB database
     * @param port MongoDB port
     * @param useAuth Use authentication?
     * @throws Exception
     */
    public void connect(String username, String password, String hostname, String database, int port, String useAuth, List<ServerAddress> replicaServers) throws Exception {
        try {
            MongoOptions options = new MongoOptions();
            options.connectionsPerHost = Configuration.getMaximumMongoDBConnections(Main.masterConfig);
            options.threadsAllowedToBlockForConnectionMultiplier = Configuration.getThreadsAllowedToBlockMultiplier(Main.masterConfig);

            // Connect to replica servers if given. Else the standard way to one server.
            if (replicaServers != null && replicaServers.size() > 0) {
                m = new Mongo(replicaServers, options);
            } else {
                ServerAddress address = new ServerAddress(hostname, port);
                m = new Mongo(address, options);
            }

            db = m.getDB(database);

            // Try to authenticate if configured.
            if (useAuth.equals("true")) {
                if(!db.authenticate(username, password.toCharArray())) {
                    throw new Exception("Could not authenticate to database '" + database + "' with user '" + username + "'.");
                }
            }
        } catch (MongoException.Network e) {
            throw new Exception("Could not connect to Mongo DB. (" + e.toString() + ")");
        }
    }

    /**
     * Returns the raw connection.
     * @return connection
     */
    public Mongo getConnection() {
        return m;
    }

    /**
     * Returns the raw database object.
     * @return database
     */
    public DB getDatabase() {
        return db;
    }

    /**
     * Get the messages collection. Lazily creates a new, capped one based on the
     * messages_collection_size from graylog2.conf if there is none.
     *
     * @return The messages collection
     */
    public DBCollection getMessagesColl() {
        if (this.messagesCollection != null) {
            return this.messagesCollection;
        }

        // Collection has not been cached yet. Do it now.
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

        coll.ensureIndex(new BasicDBObject("_id", 1));
        coll.ensureIndex(new BasicDBObject("created_at", 1));
        coll.ensureIndex(new BasicDBObject("host", 1));
        coll.ensureIndex(new BasicDBObject("streams", 1));

        coll.ensureIndex(new BasicDBObject("facility", 1));
        coll.ensureIndex(new BasicDBObject("level", 1));

        this.messagesCollection = coll;
        return coll;
    }

    public DBCollection getHistoricServerValuesColl() {
        if (this.historicServerValuesCollection != null) {
            return this.historicServerValuesCollection;
        }

        // Collection has not been cached yet. Do it now.
        DBCollection coll = null;

        // Create a capped collection if the collection does not yet exist.
        if(MongoConnection.getInstance().getDatabase().collectionExists("historic_server_values")) {
            coll = MongoConnection.getInstance().getDatabase().getCollection("historic_server_values");
        } else {
            coll = MongoConnection.getInstance()
                    .getDatabase().createCollection("historic_server_values", BasicDBObjectBuilder.start()
                    .add("capped", true)
                    .add("size", 10485760) // 10 MB
                    .add("max", 720) // Minutes. -> 12 hours.
                    .get());
        }

        coll.ensureIndex(new BasicDBObject("type", 1));
        coll.ensureIndex(new BasicDBObject("created_at", 1));

        this.historicServerValuesCollection = coll;
        return coll;
    }

}
