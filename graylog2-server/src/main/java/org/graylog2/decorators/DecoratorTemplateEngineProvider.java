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
package org.graylog2.decorators;

import com.floreysoft.jmte.Engine;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.graylog2.jmte.NamedDateRenderer;

/**
 * Provides a dedicated template engine for search response decorators. It resolves {@code search.*} variables
 * from the {@link TimeRangeTemplateContext} instead of message fields. This is a separate engine because the
 * model adaptor is engine-wide and must not affect other template users.
 */
@Singleton
public class DecoratorTemplateEngineProvider implements Provider<Engine> {
    public static final String NAME = "DecoratorTemplateEngine";

    private final Engine engine;

    @Inject
    public DecoratorTemplateEngineProvider() {
        engine = Engine.createEngine();
        engine.registerNamedRenderer(new NamedDateRenderer());
        engine.setModelAdaptor(new TimeRangeTemplateContext.ModelAdaptor());
    }

    @Override
    public Engine get() {
        return engine;
    }
}
