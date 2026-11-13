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

import com.floreysoft.jmte.DefaultModelAdaptor;
import com.floreysoft.jmte.TemplateContext;
import com.floreysoft.jmte.token.Token;

import java.util.ArrayList;
import java.util.List;

import static org.graylog2.decorators.TimeRangeTemplateContext.MODEL_KEY;
import static org.graylog2.decorators.TimeRangeTemplateContext.TEMPLATE_KEY;

/**
 * Resolves dotted {@code search.*} variables, e.g. {@code ${search.from}}, from the time range which
 * {@link TimeRangeTemplateContext#model} stores under {@value TimeRangeTemplateContext#MODEL_KEY},
 * instead of from a message field named {@value TimeRangeTemplateContext#TEMPLATE_KEY}. All other lookups, including
 * a bare {@code ${search}}, resolve from the message fields as usual.
 * <p>
 * A model adaptor applies to every template of an engine, so it must only be set on the dedicated decorator engine,
 * see {@link DecoratorTemplateEngineProvider}.
 */
class TimeRangeModelAdaptor extends DefaultModelAdaptor {
    @Override
    public Object getValue(TemplateContext context, Token token, List<String> segments, String expression) {
        if (segments.size() > 1 && TEMPLATE_KEY.equals(segments.getFirst())) {
            final List<String> redirected = new ArrayList<>(segments);
            redirected.set(0, MODEL_KEY);
            return super.getValue(context, token, redirected, expression);
        }
        return super.getValue(context, token, segments, expression);
    }
}
