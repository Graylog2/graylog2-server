package org.graylog.plugins.views.search.engine;

import org.graylog.plugins.views.ViewsModule;

public class EngineBindings extends ViewsModule {
    @Override
    protected void configure() {
        bind(SearchNormalization.class).to(PluggableSearchNormalization.class);
        bind(SearchValidation.class).to(PluggableSearchValidation.class);

        registerSearchNormalizer(DecorateQueryStringsNormalizer.class);
        registerSearchValidator(TimeRangeValidator.class);
    }
}
