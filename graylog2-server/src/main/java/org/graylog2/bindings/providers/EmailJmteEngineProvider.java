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
import com.floreysoft.jmte.NamedRenderer;
import com.floreysoft.jmte.RenderFormatInfo;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.google.common.html.HtmlEscapers;


@Singleton
public class EmailJmteEngineProvider implements Provider<Engine> {
    private Engine engine = null;

    @Inject
    public EmailJmteEngineProvider(Set<NamedRenderer> renderers) {
      engine = Engine.createEngine();

      engine.registerRenderer(String.class, new HtmlSafeRenderer());

      for (NamedRenderer renderer : renderers) {
        engine.registerNamedRenderer(renderer);
      }
    }

    @Override
    public Engine get() {
      return engine;
    }

    private static class HtmlSafeRenderer implements Renderer<String> {
      @Override
      public String render(String value, Locale locale, Map<String,Object> model) {
        String o = HtmlEscapers.htmlEscaper().escape(value);
        return o;
      }
    }
}
