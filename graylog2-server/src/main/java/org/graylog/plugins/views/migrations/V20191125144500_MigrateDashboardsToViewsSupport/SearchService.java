package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport;

import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.mongojack.JacksonDBCollection;

import javax.inject.Inject;

class SearchService {
    protected final JacksonDBCollection<Search, ObjectId> db;

    @Inject
    SearchService(MongoConnection mongoConnection, MongoJackObjectMapperProvider mapper) {
        db = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection("searches"),
                Search.class,
                ObjectId.class,
                mapper.get());
    }

    public ObjectId save(Search search) {
        return db.insert(search).getSavedId();
    }
}
