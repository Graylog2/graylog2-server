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
package org.graylog2.bindings.providers;

import com.floreysoft.jmte.Engine;
import com.floreysoft.jmte.Renderer;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import java.util.Locale;
import java.util.Map;

@Singleton
public class JsonSafeEngineProvider implements Provider<Engine> {
    private final Engine engine;

    @Inject
    public JsonSafeEngineProvider() {
        engine = Engine.createEngine();
        engine.registerRenderer(String.class, new EscapedQuoteStringRenderer());
    }
    @Override
    public Engine get() {
        return engine;
    }

    private static class EscapedQuoteStringRenderer implements Renderer<String> {

        @Override
        public String render(String s, Locale locale, Map<String, Object> map) {
            return s.replace("\"", "\\\"");
        }
    }
}
