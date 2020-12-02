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

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

public class ListOfURIsWithHostAndSchemeValidator implements Validator<List<URI>> {
    @Override
    public void validate(String name, List<URI> value) throws ValidationException {
        final List<URI> invalidUris = value.stream()
            .filter(uri -> uri.getHost() == null || uri.getScheme() == null)
            .collect(Collectors.toList());

        if (!invalidUris.isEmpty()) {
            throw new ValidationException("Parameter " + name + " must not contain URIs without host or scheme. (found " + invalidUris + ")");
        }
    }
}
