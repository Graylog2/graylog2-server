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
package org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.savedsearch;

import com.google.common.collect.Streams;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;

import javax.inject.Inject;
import java.util.stream.Stream;

public class SavedSearchService {
    private static final String COLLECTION_NAME = "saved_searches";
    private final JacksonDBCollection<SavedSearch, ObjectId> db;

    @Inject
    public SavedSearchService(MongoConnection mongoConnection, MongoJackObjectMapperProvider mapper) {
        this.db = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection(COLLECTION_NAME),
                SavedSearch.class,
                ObjectId.class,
                mapper.get());
    }

    public Stream<SavedSearch> streamAll() {
        final DBCursor<SavedSearch> cursor = db.find(DBQuery.empty());
        return Streams.stream(cursor.iterator()).onClose(cursor::close);
    }
}

