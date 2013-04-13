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

import java.net.UnknownHostException;
import java.util.List;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.MongoOptions;
import com.mongodb.ServerAddress;

/**
 * MongoDB connection singleton
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public final class MongoConnection {
    private Mongo m;
    private DB db;

    private DBCollection messageCountsCollection;

    private String username;

    private List<ServerAddress> replicaServers;

    private int threadsAllowedToBlockMultiplier;

    private boolean useAuth;

    private int maxConnections;

    private String database;

    private String password;

    private String host;

    private int port;

    public MongoConnection() {
    }

    /**
     * Connect the instance.
     */
    public synchronized Mongo connect() {
        if (m == null) {
            MongoOptions options = new MongoOptions();
            options.connectionsPerHost = maxConnections;
            options.threadsAllowedToBlockForConnectionMultiplier = threadsAllowedToBlockMultiplier;

            try {

                // Connect to replica servers if given. Else the standard way to one server.
                if (replicaServers != null && replicaServers.size() > 0) {
                    m = new Mongo(replicaServers, options);
                } else {
                    ServerAddress address = new ServerAddress(host, port);
                    m = new Mongo(address, options);
                }
                db = m.getDB(database);

                // Try to authenticate if configured.
                if (useAuth) {
                    if(!db.authenticate(username, password.toCharArray())) {
                        throw new RuntimeException("Could not authenticate to database '" + database + "' with user '" + username + "'.");
                    }
                }
            } catch (MongoException.Network e) {
                throw e;
            } catch (UnknownHostException e) {
                throw new RuntimeException("Cannot resolve host name for MongoDB", e);
            }
        }
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
     * Get the message_counts collection. Lazily checks if correct indizes are set.
     *
     * @return The messages collection
     */
    public DBCollection getMessageCountsColl() {
        if (this.messageCountsCollection != null) {
            return this.messageCountsCollection;
        }

        // Collection has not been cached yet. Do it now.
        DBCollection coll = getDatabase().getCollection("message_counts");

        coll.ensureIndex(new BasicDBObject("timestamp", 1));

        this.messageCountsCollection = coll;
        return coll;
    }

    public void setUser(String mongoUser) {
        this.username = mongoUser;
    }

    public void setReplicaSet(List<ServerAddress> mongoReplicaSet) {
        this.replicaServers = mongoReplicaSet;
    }

    public void setThreadsAllowedToBlockMultiplier(
            int mongoThreadsAllowedToBlockMultiplier) {
                this.threadsAllowedToBlockMultiplier = mongoThreadsAllowedToBlockMultiplier;
    }

    public void setUseAuth(boolean mongoUseAuth) {
        this.useAuth = mongoUseAuth;
    }

    public void setMaxConnections(int mongoMaxConnections) {
        this.maxConnections = mongoMaxConnections;
    }

    public void setDatabase(String mongoDatabase) {
        this.database = mongoDatabase;
    }

    public void setPassword(String mongoPassword) {
        this.password = mongoPassword;
    }

    public void setHost(String mongoHost) {
        this.host = mongoHost;
    }

    public void setPort(int mongoPort) {
        this.port = mongoPort;
    }

}
