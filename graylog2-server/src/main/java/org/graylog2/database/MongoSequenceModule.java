package org.graylog2.database;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

/**
 * Guice module for {@link MongoSequenceService}. Binds the service as a singleton
 * and initializes the empty {@link SequenceTopics} multibinder. Other modules
 * (including plugin modules) add topic names to this multibinder.
 */
public class MongoSequenceModule extends AbstractModule {
    @Override
    protected void configure() {
        // Initialize the multibinder (even if no topics are registered in this module)
        Multibinder.newSetBinder(binder(), String.class, SequenceTopics.class);

        bind(MongoSequenceService.class);
    }
}
