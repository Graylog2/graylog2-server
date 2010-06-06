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
 * MongoConnection.java: Lennart Koopmann <lennart@scopeport.org> | Jun 6, 2010 1:36:19 PM
 */

package graylog2.database;

import com.mongodb.Mongo;
import com.mongodb.DB;
import com.mongodb.MongoException;

public class MongoConnection {
    private static MongoConnection INSTANCE;

    private Mongo m = null;
    private DB db = null;

    private MongoConnection() {}

    public synchronized static MongoConnection getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MongoConnection();
        }
        return INSTANCE;
    }

    public void connect(String username, String password, String hostname, String database, int port) throws Exception {
        try {
            this.m = new Mongo(hostname, port);
            this.db = m.getDB(database);

            // Try to authenticate.
            if(!db.authenticate(username, password.toCharArray())) {
                throw new Exception("Could not authenticate to database '" + database + "' with user '" + username + "'.");
            }
        } catch (MongoException.Network e) {
            throw new Exception("Could not connect to Mongo DB.");
        }
    }

    public Mongo getConnection() {
        return this.m;
    }

    public DB getDatabase() {
        return this.db;
    }
}
