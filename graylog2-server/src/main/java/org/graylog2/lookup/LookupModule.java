package org.graylog2.lookup;

import org.graylog2.lookup.adapters.RandomDataAdapter;
import org.graylog2.lookup.caches.GuavaLookupCache;
import org.graylog2.lookup.caches.NullCache;
import org.graylog2.plugin.inject.Graylog2Module;

public class LookupModule extends Graylog2Module {

    @Override
    protected void configure() {
        bind(LookupTableService.class).asEagerSingleton();

        installLookupCache(NullCache.NAME,
                NullCache.class,
                NullCache.Factory.class,
                NullCache.Config.class);

        installLookupCache(GuavaLookupCache.NAME,
                GuavaLookupCache.class,
                GuavaLookupCache.Factory.class,
                GuavaLookupCache.Config.class);

        installLookupDataAdapter(RandomDataAdapter.NAME,
                RandomDataAdapter.class,
                RandomDataAdapter.Factory.class,
                RandomDataAdapter.Config.class);

    }

}
