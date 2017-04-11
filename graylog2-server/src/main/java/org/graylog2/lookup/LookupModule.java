package org.graylog2.lookup;

import org.graylog2.plugin.inject.Graylog2Module;

public class LookupModule extends Graylog2Module {

    @Override
    protected void configure() {
        bind(LookupTableService.class).asEagerSingleton();

        lookupCacheBinder().addBinding(GuavaLookupCache.NAME).to(GuavaLookupCache.class);
        lookupDataAdapterBinder().addBinding(DevZeroDataAdapter.NAME).to(DevZeroDataAdapter.class);

        jacksonSubTypesBinder().addBinding(DevZeroDataAdapter.NAME).toInstance((Class) DevZeroDataAdapter.Config.class);
        jacksonSubTypesBinder().addBinding(GuavaLookupCache.NAME).toInstance((Class)GuavaLookupCache.Config.class);
    }
}
