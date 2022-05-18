package org.graylog.plugins.views.search.engine;

import org.graylog.plugins.views.ViewsModule;
import org.graylog.plugins.views.search.engine.normalization.DecorateQueryStringsNormalizer;
import org.graylog.plugins.views.search.engine.normalization.PluggableSearchNormalization;
import org.graylog.plugins.views.search.engine.normalization.SearchNormalization;
import org.graylog.plugins.views.search.engine.validation.PluggableSearchValidation;
import org.graylog.plugins.views.search.engine.validation.SearchValidation;
import org.graylog.plugins.views.search.engine.validation.TimeRangeValidator;

public class EngineBindings extends ViewsModule {
    @Override
    protected void configure() {
        bind(SearchNormalization.class).to(PluggableSearchNormalization.class);
        bind(SearchValidation.class).to(PluggableSearchValidation.class);

        registerSearchNormalizer(DecorateQueryStringsNormalizer.class);
        registerSearchValidator(TimeRangeValidator.class);
    }
}
