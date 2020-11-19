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
package org.graylog2.configuration.validators;

import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.Validator;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class HttpOrHttpsSchemeValidator implements Validator<String> {

    private static final List<String> validScheme = Arrays.asList("http", "https");

    @Override
    public void validate(String name, String value) throws ValidationException {
        if (!validScheme.contains(value.toLowerCase(Locale.ENGLISH))) {
            throw new ValidationException(String.format(Locale.ENGLISH, "Parameter " + name + " must be one of [%s]", String.join(",", validScheme)));
        }
    }
}
