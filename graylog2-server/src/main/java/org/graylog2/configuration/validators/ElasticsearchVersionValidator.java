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
import org.graylog2.storage.SearchVersion;

import java.util.List;

import static org.graylog2.storage.SearchVersion.Distribution.ELASTICSEARCH;
import static org.graylog2.storage.SearchVersion.Distribution.OPENSEARCH;

public class ElasticsearchVersionValidator implements Validator<SearchVersion> {
    public static final List<SearchVersionRange> SUPPORTED_ES_VERSIONS = ImmutableList.of(
            SearchVersionRange.of(OPENSEARCH, "^1.0.0"),
            SearchVersionRange.of(ELASTICSEARCH, "^6.0.0"),
            SearchVersionRange.of(ELASTICSEARCH, "^7.0.0")
    );


    @Override
    public void validate(String name, SearchVersion value) throws ValidationException {
        if (SUPPORTED_ES_VERSIONS.stream().noneMatch(value::satisfies)) {
            throw new ValidationException("Invalid Search version specified in " + name + ": " + value
                    + ". Supported versions: " + SUPPORTED_ES_VERSIONS);
        }
    }
}
