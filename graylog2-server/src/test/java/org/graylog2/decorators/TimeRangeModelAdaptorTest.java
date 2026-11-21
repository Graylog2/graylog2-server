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
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.decorators.TimeRangeTemplateContext.MODEL_KEY;

class TimeRangeModelAdaptorTest {
    private static final Map<String, Object> TIME_RANGE = Map.of("from_ms", 1000L, "to_ms", 2000L);

    private final Engine engine = createEngine();

    private static Engine createEngine() {
        final Engine engine = Engine.createEngine();
        engine.setModelAdaptor(new TimeRangeModelAdaptor());
        return engine;
    }

    private String render(String template, Map<String, Object> model) {
        return engine.transform(template, Locale.ENGLISH, model);
    }

    private static Map<String, Object> model(Map<String, Object> messageFields) {
        final Map<String, Object> model = new HashMap<>(messageFields);
        model.put(MODEL_KEY, TIME_RANGE);
        return model;
    }

    @Test
    void resolvesDottedSearchVariablesFromTimeRange() {
        assertThat(render("${search.from_ms}-${search.to_ms}", model(Map.of()))).isEqualTo("1000-2000");
    }

    @Test
    void dottedSearchVariablesIgnoreMessageFieldNamedSearch() {
        final Map<String, Object> model = model(Map.of("search", Map.of("from_ms", 42L)));

        assertThat(render("${search.from_ms}", model)).isEqualTo("1000");
    }

    @Test
    void bareSearchResolvesToMessageField() {
        assertThat(render("${search}", model(Map.of("search", "foo")))).isEqualTo("foo");
        assertThat(render("${search}", model(Map.of()))).isEmpty();
    }

    @Test
    void unknownSearchPropertyRendersEmpty() {
        assertThat(render("${search.foo}", model(Map.of()))).isEmpty();
    }

    @Test
    void missingTimeRangeRendersEmpty() {
        assertThat(render("${search.from_ms}", Map.of("search", Map.of("from_ms", 42L)))).isEmpty();
    }

    @Test
    void conditionsUseTimeRange() {
        final String template = "${if search.from_ms}yes${else}no${end}";

        assertThat(render(template, model(Map.of()))).isEqualTo("yes");
        assertThat(render(template, Map.of())).isEqualTo("no");
    }

    @Test
    void otherDottedVariablesResolveFromMessageFields() {
        assertThat(render("${other.value}", model(Map.of("other", Map.of("value", "x"))))).isEqualTo("x");
    }
}
