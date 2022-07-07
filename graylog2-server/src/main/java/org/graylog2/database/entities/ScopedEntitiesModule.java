package org.graylog2.database.entities;

import com.google.inject.multibindings.Multibinder;
import org.graylog2.plugin.PluginModule;

public class ScopedEntitiesModule extends PluginModule {
    @Override
    protected void configure() {

        Multibinder<EntityScope> scopeBinder = Multibinder.newSetBinder(binder(), EntityScope.class);
        scopeBinder.addBinding().to(DefaultEntityScope.class);

    }
}
