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
package org.graylog2.shared.rest.documentation.openapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.shared.bindings.providers.config.ObjectMapperConfiguration;

/**
 * Singleton holder for an {@link ObjectMapperConfiguration} to configure Jackson {@link ObjectMapper}
 * instances outside of Graylog's Guice context (e.g. Swagger's internal ObjectMapper).
 */
public class ObjectMapperConfigurer {
    private static volatile ObjectMapperConfigurer instance;

    private final ObjectMapperConfiguration configuration;

    private ObjectMapperConfigurer(ObjectMapperConfiguration configuration) {
        this.configuration = configuration;
    }

    public static synchronized void initialize(ObjectMapperConfiguration configuration) {
        if (instance != null) {
            throw new IllegalStateException("ObjectMapperConfigurer has already been initialized");
        }
        instance = new ObjectMapperConfigurer(configuration);
    }

    public static synchronized boolean isInitialized() {
        return instance != null;
    }

    public static ObjectMapperConfigurer getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ObjectMapperConfigurer has not been initialized yet");
        }
        return instance;
    }

    public void configure(ObjectMapper objectMapper) {
        configuration.configure(objectMapper);
    }
}
