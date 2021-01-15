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
package org.graylog2.configuration.converters;

import com.github.joschi.jadconfig.Converter;
import com.github.joschi.jadconfig.ParameterException;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.google.common.base.Preconditions.checkNotNull;

public class URIListConverter implements Converter<List<URI>> {
    private static final String SEPARATOR = ",";

    @Override
    public List<URI> convertFrom(String value) {
        if (value == null) {
            throw new ParameterException("URI List must not be null.");
        }

        final Iterable<String> splittedUris = Splitter.on(SEPARATOR)
            .omitEmptyStrings()
            .trimResults()
            .split(value);

        return StreamSupport.stream(splittedUris.spliterator(), false)
            .map(this::constructURIFromString)
            .collect(Collectors.toList());
    }

    @Override
    public String convertTo(List<URI> value) {
        if (value == null) {
            throw new ParameterException("URI List must not be null.");
        }

        return Joiner.on(SEPARATOR)
            .skipNulls()
            .join(value);
    }

    private URI constructURIFromString(String value) {
        try {
            return new URI(value);
        } catch (URISyntaxException e) {
            throw new ParameterException(e.getMessage(), e);
        }
    }
}
