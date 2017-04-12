package org.graylog2.lookup;

import com.google.inject.assistedinject.FactoryModuleBuilder;

import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.plugin.lookup.LookupDataAdapter;
import org.graylog2.plugin.lookup.LookupDataAdapterConfiguration;

public class LookupModule extends Graylog2Module {

    @Override
    protected void configure() {
        bind(LookupTableService.class).asEagerSingleton();

        installLookupCache(GuavaLookupCache.NAME,
                GuavaLookupCache.class,
                GuavaLookupCache.Factory.class,
                GuavaLookupCache.Config.class);

        installLookupDataAdapter(DevZeroDataAdapter.NAME,
                DevZeroDataAdapter.class,
                DevZeroDataAdapter.Factory.class,
                DevZeroDataAdapter.Config.class);

    }

}
