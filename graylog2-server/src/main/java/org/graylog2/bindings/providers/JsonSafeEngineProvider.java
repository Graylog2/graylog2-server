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
import org.apache.commons.lang.StringEscapeUtils;

import java.util.Locale;
import java.util.Map;

@Singleton
public class JsonSafeEngineProvider implements Provider<Engine> {
    private final Engine engine;

    @Inject
    public JsonSafeEngineProvider() {
        engine = Engine.createEngine();
        engine.registerRenderer(String.class, new JsonSafeRenderer());
    }
    @Override
    public Engine get() {
        return engine;
    }

    private static class JsonSafeRenderer implements Renderer<String> {

        @Override
        public String render(String s, Locale locale, Map<String, Object> map) {
            // Current version of Apache Commons does not have native support for escapeJson. However,
            // https://commons.apache.org/proper/commons-text/javadocs/api-release/org/apache/commons/text/StringEscapeUtils.html#escapeJson(java.lang.String)
            // current Apache Commons docs states:
            // 'The only difference between Java strings and Json strings is that in Json, forward-slash (/) is escaped.'
            // So we use escapeJava and tack on an extra String.replace() call to escape forward slashes.
            return StringEscapeUtils.escapeJava(s).replace("/", "\\/");
        }
    }
}
