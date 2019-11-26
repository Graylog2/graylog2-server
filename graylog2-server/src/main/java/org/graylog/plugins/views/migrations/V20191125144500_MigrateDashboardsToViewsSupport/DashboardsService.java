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
package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport;

import com.google.common.collect.Streams;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoConnection;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;

import javax.inject.Inject;
import java.util.stream.Stream;

class DashboardsService {
    private static final String COLLECTION_NAME = "dashboards";
    private final JacksonDBCollection<Dashboard, ObjectId> db;

    @Inject
    DashboardsService(MongoConnection mongoConnection, ObjectMapperProvider mapper) {
        this.db = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection(COLLECTION_NAME),
                Dashboard.class,
                ObjectId.class,
                mapper.get());
    }

    Stream<Dashboard> streamAll() {
        final DBCursor<Dashboard> cursor = db.find(DBQuery.empty());
        return Streams.stream((Iterable<Dashboard>) cursor).onClose(cursor::close);
    }
}
