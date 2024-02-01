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
package org.graylog.plugins.views.search.engine.suggestions;

import com.github.joschi.jadconfig.Converter;
import com.github.joschi.jadconfig.ParameterException;

import java.util.Arrays;
import java.util.Locale;

public class FieldValueSuggestionModeConverter implements Converter<FieldValueSuggestionMode> {

    @Override
    public FieldValueSuggestionMode convertFrom(String value) {
        try {
            return FieldValueSuggestionMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ParameterException("Parameter should have one of the allowed values: " + Arrays.toString(FieldValueSuggestionMode.values()) + " (found: " + value + ")");
        }
    }

    @Override
    public String convertTo(FieldValueSuggestionMode value) {
        return value.name();
    }
}
