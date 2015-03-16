/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.database;

import com.mongodb.CommandFailureException;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import org.graylog2.configuration.MongoDbConfiguration;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.UnknownHostException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * MongoDB connection singleton
 */
@Singleton
public class MongoConnectionImpl implements MongoConnection {
    private final MongoClientURI mongoClientURI;

    private MongoClient m = null;
    private DB db = null;

    @Inject
    public MongoConnectionImpl(final MongoDbConfiguration configuration) {
        this(configuration.getMongoClientURI());
    }

    MongoConnectionImpl(MongoClientURI mongoClientURI) {
        this.mongoClientURI = checkNotNull(mongoClientURI);
    }

    /**
     * Connect the instance.
     */
    @Override
    public synchronized MongoClient connect() {
        if (m == null) {
            try {
                m = new MongoClient(mongoClientURI);
                db = m.getDB(mongoClientURI.getDatabase());
                db.setWriteConcern(WriteConcern.SAFE);
            } catch (UnknownHostException e) {
                throw new RuntimeException("Cannot resolve host name for MongoDB", e);
            }
        }

        try {
            db.command("{ ping: 1 }");
        } catch (CommandFailureException e) {
            if(e.getCode() == 18) {
                throw new MongoException("Couldn't connect to MongoDB. Please check the authentication credentials.", e);
            } else {
                throw new MongoException("Couldn't connect to MongoDB: " + e.getMessage(), e);
            }
        }

        return m;
    }

    /**
     * Returns the raw database object.
     *
     * @return database
     */
    @Override
    public DB getDatabase() {
        return db;
    }
}
