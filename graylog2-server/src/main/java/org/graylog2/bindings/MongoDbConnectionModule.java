package org.graylog2.bindings;

import com.google.inject.AbstractModule;
import org.graylog2.bindings.providers.MongoConnectionProvider;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoConnection;

/**
 * Provides a basic MongoDB connection ready to be used with mongojack
 */
public class MongoDbConnectionModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(MongoConnection.class).toProvider(MongoConnectionProvider.class);
        bind(MongoCollections.class).asEagerSingleton();
        bind(MongoJackObjectMapperProvider.class);
    }
}
