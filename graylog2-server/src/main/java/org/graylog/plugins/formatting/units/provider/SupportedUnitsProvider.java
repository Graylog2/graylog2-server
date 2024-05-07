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
import jakarta.validation.constraints.NotNull;
import org.graylog.plugins.formatting.units.model.BaseUnit;
import org.graylog.plugins.formatting.units.model.Conversion;
import org.graylog.plugins.formatting.units.model.DerivedUnit;
import org.graylog.plugins.formatting.units.model.SupportedUnits;
import org.graylog.plugins.formatting.units.model.Unit;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.graylog.plugins.formatting.units.model.Conversion.ConversionAction.DIVIDE;
import static org.graylog.plugins.formatting.units.model.Conversion.ConversionAction.MULTIPLY;


@Singleton
public class SupportedUnitsProvider implements Provider<SupportedUnits> {

    private static final List<Unit> UNITS_FALLBACK = List.of(
            new BaseUnit("B", "byte", "size"),
            new DerivedUnit("kB", "kilobyte", "size", new Conversion(1000, MULTIPLY)),
            new DerivedUnit("MB", "megabyte", "size", new Conversion(1000 * 1000, MULTIPLY)),
            new DerivedUnit("GB", "gigabyte", "size", new Conversion(1000 * 1000 * 1000, MULTIPLY)),

            new DerivedUnit("ns", "nanosecond", "time", new Conversion(1000 * 1000 * 1000, DIVIDE)),
            new DerivedUnit("μs", "microsecond", "time", new Conversion(1000 * 1000, DIVIDE)),
            new DerivedUnit("ms", "millisecond", "time", new Conversion(1000, DIVIDE)),
            new BaseUnit("s", "second", "time"),
            new DerivedUnit("min", "minute", "time", new Conversion(60, MULTIPLY)),
            new DerivedUnit("h", "hour", "time", new Conversion(60 * 60, MULTIPLY)),
            new DerivedUnit("d", "day", "time", new Conversion(24 * 60 * 60, MULTIPLY)),
            new DerivedUnit("m", "month", "time", new Conversion(30 * 24 * 60 * 60, MULTIPLY)),
            
            new BaseUnit("%", "percent", "percent")
    );
    private static final String SUPPORTED_UNITS_RES_FILE_LOCATION = "/units/supported_units.json";
    private SupportedUnits supportedUnitsMemoized;

    @Inject
    public SupportedUnitsProvider(final ObjectMapper objectMapper) {
        try {
            this.supportedUnitsMemoized = loadFromFile(objectMapper);
        } catch (Exception ex) {
            this.supportedUnitsMemoized = useFallback();
        }
    }

    public SupportedUnits get() {
        return supportedUnitsMemoized;
    }

    private SupportedUnits loadFromFile(final ObjectMapper objectMapper) throws IOException {
        return objectMapper.readValue(getClass().getResource(SUPPORTED_UNITS_RES_FILE_LOCATION), SupportedUnits.class);
    }

    @NotNull
    private SupportedUnits useFallback() {
        Map<String, List<Unit>> groupedByUnitType = new HashMap<>();
        UNITS_FALLBACK.forEach(unit -> {
            groupedByUnitType.putIfAbsent(unit.unitType(), new LinkedList<>());
            final List<Unit> unitsOfTheSameUnitType = groupedByUnitType.get(unit.unitType());
            unitsOfTheSameUnitType.add(unit);
        });

        return new SupportedUnits(groupedByUnitType);
    }

}
