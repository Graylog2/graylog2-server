package org.graylog.plugins.views.search.engine;

import org.graylog.plugins.views.ViewsModule;

public class EngineBindings extends ViewsModule {
    @Override
    protected void configure() {
        bind(SearchNormalization.class).to(PluggableSearchNormalization.class);
        bind(SearchValidator.class).to(DefaultSearchValidator.class);

        registerSearchNormalizer(DecorateQueryStringsNormalizer.class);
    }
}
