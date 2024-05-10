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
package org.graylog.plugins.formatting.units.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.graylog.plugins.formatting.units.model.SupportedUnits;

import java.io.IOException;


@Singleton
public class SupportedUnitsProvider implements Provider<SupportedUnits> {

    private static final String SUPPORTED_UNITS_RES_FILE_LOCATION = "/units/supported_units.json";
    private SupportedUnits supportedUnitsMemoized;

    private final ObjectMapper objectMapper;

    @Inject
    public SupportedUnitsProvider(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SupportedUnits get() {
        if (this.supportedUnitsMemoized == null) {
            try {
                this.supportedUnitsMemoized = loadFromFile(objectMapper);
            } catch (Exception ex) {
                throw new IllegalStateException("Graylog cannot start because the file with a list of supported units is missing or corrupted : " + ex.getMessage());
            }
        }
        return supportedUnitsMemoized;
    }

    private SupportedUnits loadFromFile(final ObjectMapper objectMapper) throws IOException {
        return objectMapper.readValue(getClass().getResource(SUPPORTED_UNITS_RES_FILE_LOCATION), SupportedUnits.class);
    }

}
