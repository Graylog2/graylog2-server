package org.graylog2.lookup.db;

import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.lookup.LookupDataAdapter;

import javax.inject.Inject;

public class LookupStorageFactory {

    private final MongoConnection mongoConnection;
    private final MongoJackObjectMapperProvider objectMapperProvider;

    @Inject
    public LookupStorageFactory(MongoConnection mongoConnection,
                                MongoJackObjectMapperProvider objectMapperProvider) {
        this.mongoConnection = mongoConnection;
        this.objectMapperProvider = objectMapperProvider;
    }

    public LookupStorage getStorage(LookupDataAdapter dataAdapter) {
        return new LookupStorage(mongoConnection, objectMapperProvider, dataAdapter.id());
    }
}
