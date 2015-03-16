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
package org.graylog2.bindings.providers;

import org.graylog2.configuration.MongoDbConfiguration;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.MongoConnectionImpl;

import javax.inject.Inject;
import javax.inject.Provider;

public class MongoConnectionProvider implements Provider<MongoConnection> {
    private static MongoConnection mongoConnection = null;

    @Inject
    public MongoConnectionProvider(MongoDbConfiguration configuration) {
        if (mongoConnection == null) {
            mongoConnection = new MongoConnectionImpl(configuration);

            mongoConnection.connect();
        }
    }

    @Override
    public MongoConnection get() {
        return mongoConnection;
    }
}
