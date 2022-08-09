/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
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
        // Triggering set binder explicitly, so no injection errors are being caused if no implementation is bound.
        searchPostValidationNormalizerBinder();
        registerSearchValidator(TimeRangeValidator.class);
    }
}
