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

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
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
public class MongoConnection {
    private final MongoClientURI mongoClientURI;

    private MongoClient m = null;
    private DB db = null;

    @Inject
    public MongoConnection(final MongoDbConfiguration configuration) {
        this(configuration.getMongoClientURI());
    }

    MongoConnection(MongoClientURI mongoClientURI) {
        this.mongoClientURI = checkNotNull(mongoClientURI);
    }

    /**
     * Connect the instance.
     */
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
}
