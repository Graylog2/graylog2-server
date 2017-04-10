package org.graylog2.lookup;

import org.graylog2.plugin.inject.Graylog2Module;

public class LookupModule extends Graylog2Module {

    @Override
    protected void configure() {
        bind(LookupTableService.class).asEagerSingleton();
    }
}
