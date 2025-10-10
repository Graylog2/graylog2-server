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

/**
 * Configures Swagger's internal ObjectMapper with Graylog's application-wide Jackson configuration.
 * <p>
 * NOTE: Swagger instantiates this class via reflection using a no-arg constructor, so dependency
 * injection cannot be used. Configuration is retrieved from {@link ObjectMapperConfigurer}.
 * </p>
 */
public class CustomObjectMapperProcessor implements ObjectMapperProcessor {

    @Override
    public void processJsonObjectMapper(ObjectMapper swaggerMapper) {
        ObjectMapperConfigurer.getInstance().configure(swaggerMapper);
    }
}
