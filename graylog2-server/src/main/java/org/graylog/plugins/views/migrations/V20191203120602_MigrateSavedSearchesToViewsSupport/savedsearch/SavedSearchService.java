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
    SavedSearchService(MongoConnection mongoConnection, MongoJackObjectMapperProvider mapper) {
        this.db = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection(COLLECTION_NAME),
                SavedSearch.class,
                ObjectId.class,
                mapper.get());
    }

    public Stream<SavedSearch> streamAll() {
        final DBCursor<SavedSearch> cursor = db.find(DBQuery.empty());
        return Streams.stream((Iterable<SavedSearch>) cursor).onClose(cursor::close);
    }
}

