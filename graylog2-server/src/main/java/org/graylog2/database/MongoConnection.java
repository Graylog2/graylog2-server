/**
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
 */
package org.graylog2.database;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientOptions.Builder;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import org.graylog2.Configuration;

import java.net.UnknownHostException;
import java.util.List;

/**
 * MongoDB connection singleton
 */
@Singleton
public class MongoConnection {
    private MongoClient m;
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

    @Inject
    public MongoConnection(final Configuration configuration) {
        this(
                configuration.getMongoDatabase(),
                configuration.getMongoHost(),
                configuration.getMongoPort(),
                configuration.getMongoReplicaSet(),
                configuration.isMongoUseAuth(),
                configuration.getMongoUser(),
                configuration.getMongoPassword(),
                configuration.getMongoMaxConnections(),
                configuration.getMongoThreadsAllowedToBlockMultiplier());
    }

    public MongoConnection(final String database,
                           final String host,
                           final int port,
                           final List<ServerAddress> replicaServers,
                           final boolean useAuth,
                           final String username,
                           final String password,
                           final int maxConnections,
                           final int threadsAllowedToBlockMultiplier) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.replicaServers = replicaServers;
        this.useAuth = useAuth;
        this.username = username;
        this.password = password;
        this.maxConnections = maxConnections;
        this.threadsAllowedToBlockMultiplier = threadsAllowedToBlockMultiplier;
    }

    /**
     * Connect the instance.
     */
    public synchronized MongoClient connect() {
        if (m == null) {
            Builder options = new MongoClientOptions.Builder();
            options.connectionsPerHost(maxConnections);
            options.threadsAllowedToBlockForConnectionMultiplier(threadsAllowedToBlockMultiplier);

            try {

                // Connect to replica servers if given. Else the standard way to one server.
                if (replicaServers != null && replicaServers.size() > 0) {
                    m = new MongoClient(replicaServers, options.build());
                } else {
                    ServerAddress address = new ServerAddress(host, port);
                    m = new MongoClient(address, options.build());
                }
                db = m.getDB(database);
                db.setWriteConcern(WriteConcern.SAFE);

                // Try to authenticate if configured.
                if (useAuth) {
                    if (!db.authenticate(username, password.toCharArray())) {
                        throw new RuntimeException("Could not authenticate to database '" + database + "' with user '" + username + "'.");
                    }
                }
            } catch (UnknownHostException e) {
                throw new RuntimeException("Cannot resolve host name for MongoDB", e);
            }
        }
        return m;
    }

    /**
     * Returns the raw database object.
     *
     * @return database
     */
    public DB getDatabase() {
        return db;
    }


    /**
     * Get the message_counts collection. Lazily checks if correct indices are set.
     *
     * @return The messages collection
     */
    public DBCollection getMessageCountsColl() {
        if (this.messageCountsCollection != null) {
            return this.messageCountsCollection;
        }

        // Collection has not been cached yet. Do it now.
        DBCollection coll = getDatabase().getCollection("message_counts");

        coll.createIndex(new BasicDBObject("timestamp", 1));

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
