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
package org.graylog2.inputs.converters;

import org.graylog2.plugin.inputs.Converter;

import java.util.Locale;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class UppercaseConverter extends Converter {

    public UppercaseConverter(Map<String, Object> config) {
        super(Type.UPPERCASE, config);
    }

    @Override
    public Object convert(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        return value.toUpperCase(Locale.ENGLISH);
    }

    @Override
    public boolean buildsMultipleFields() {
        return false;
    }
}
