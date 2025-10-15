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
import io.swagger.v3.oas.integration.api.ObjectMapperProcessor;
import jakarta.inject.Inject;
import org.graylog2.shared.bindings.providers.config.ObjectMapperConfiguration;

/**
 * Configures Swagger's internal ObjectMapper with Graylog's application-wide Jackson configuration.
 */
public class CustomObjectMapperProcessor implements ObjectMapperProcessor {

    private final ObjectMapperConfiguration objectMapperConfiguration;

    @Inject
    public CustomObjectMapperProcessor(ObjectMapperConfiguration objectMapperConfiguration) {
        this.objectMapperConfiguration = objectMapperConfiguration;
    }

    @Override
    public void processJsonObjectMapper(ObjectMapper swaggerMapper) {
        objectMapperConfiguration.configure(swaggerMapper);
    }
}
