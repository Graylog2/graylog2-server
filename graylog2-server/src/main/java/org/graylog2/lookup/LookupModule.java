package org.graylog2.lookup;

import com.google.inject.assistedinject.FactoryModuleBuilder;

import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.plugin.lookup.LookupDataAdapter;
import org.graylog2.plugin.lookup.LookupDataAdapterConfiguration;

public class LookupModule extends Graylog2Module {

    @Override
    protected void configure() {
        bind(LookupTableService.class).asEagerSingleton();

        lookupCacheBinder().addBinding(GuavaLookupCache.NAME).to(GuavaLookupCache.class);
        jacksonSubTypesBinder().addBinding(GuavaLookupCache.NAME).toInstance((Class) GuavaLookupCache.Config.class);

        installLookupDataAdapter(DevZeroDataAdapter.NAME,
                DevZeroDataAdapter.class,
                DevZeroDataAdapter.Factory.class,
                DevZeroDataAdapter.Config.class);

    }

    private void installLookupDataAdapter(String name,
                                          Class<? extends LookupDataAdapter> adapterClass,
                                          Class<? extends LookupDataAdapter.Factory> factoryClass,
                                          Class<? extends LookupDataAdapterConfiguration> configClass) {
        install(new FactoryModuleBuilder().implement(LookupDataAdapter.class, adapterClass).build(factoryClass));
        lookupDataAdapterBinder().addBinding(name).to(factoryClass);
        jacksonSubTypesBinder().addBinding(DevZeroDataAdapter.NAME).toInstance(configClass);
    }
}
