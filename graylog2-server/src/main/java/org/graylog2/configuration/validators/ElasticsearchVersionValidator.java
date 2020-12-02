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
import com.google.common.collect.ImmutableList;
import org.graylog2.plugin.Version;

import java.util.List;

public class ElasticsearchVersionValidator implements Validator<Version> {
    private static final List<Version> SUPPORTED_ES_VERSIONS = ImmutableList.of(
            Version.from(6, 0, 0),
            Version.from(7, 0, 0)
    );

    @Override
    public void validate(String name, Version value) throws ValidationException {
        if (!SUPPORTED_ES_VERSIONS.contains(value)) {
            throw new ValidationException("Invalid Elasticsearch version specified in " + name + ": " + value
                    + ". Supported versions: " + SUPPORTED_ES_VERSIONS);
        }
    }
}
