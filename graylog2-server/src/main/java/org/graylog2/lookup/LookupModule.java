package org.graylog2.lookup;

import org.graylog2.plugin.inject.Graylog2Module;

public class LookupModule extends Graylog2Module {

    @Override
    protected void configure() {
        bind(LookupTableService.class).asEagerSingleton();

        lookupCacheBinder().addBinding(GuavaLookupCache.NAME).to(GuavaLookupCache.class);
        lookupDataAdapterBinder().addBinding(DevZeroDataAdapter.NAME).to(DevZeroDataAdapter.class);
    }
}
