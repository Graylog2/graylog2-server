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
package org.graylog2.configuration.retrieval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Inject;

import java.util.Map;
import java.util.Optional;

public class SingleConfigurationValueRetriever {

    private final ObjectMapper objectMapper;

    @Inject
    public SingleConfigurationValueRetriever(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<Object> retrieveSingleValue(final Object jsonObject, final String valueName) {
        final Map<String, Object> map = objectMapper.convertValue(jsonObject, new TypeReference<>() {});
        return Optional.ofNullable(map.get(valueName));
    }
}
