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
package org.graylog.plugins.formatting.units.db;

import org.graylog.plugins.formatting.units.model.BaseUnit;
import org.graylog.plugins.formatting.units.model.BaseUnitView;
import org.graylog.plugins.formatting.units.model.Conversion;
import org.graylog.plugins.formatting.units.model.DerivedUnit;
import org.graylog.plugins.formatting.units.model.Unit;
import org.graylog.plugins.formatting.units.model.UnitView;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoConnection;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.graylog.plugins.formatting.units.model.Conversion.ConversionAction.DIVIDE;
import static org.graylog.plugins.formatting.units.model.Conversion.ConversionAction.MULTIPLY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class UnitServiceTest {

    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    private UnitService toTest;

    @Before
    public void setUp() throws Exception {
        final MongoConnection mongoConnection = mongodb.mongoConnection();
        final MongoJackObjectMapperProvider objectMapperProvider = new MongoJackObjectMapperProvider(new ObjectMapperProvider().get());
        toTest = new UnitService(new MongoCollections(objectMapperProvider, mongoConnection));
    }

    @Test
    public void cannotSaveTwoBaseUnitsForTheSameUnitType() {
        Unit meter = new BaseUnit("m", "meter", "length", true, true);
        Unit yard = new BaseUnit("y", "yard", "length", false, false);
        toTest.save(meter);
        assertThrows(Exception.class, () -> toTest.save(yard));
    }

    @Test
    public void cannotSaveTwoUnitsWithTheSameAbbreviationForTheSameUnitType() {
        Unit meter = new BaseUnit("m", "meter", "length", true, true);
        Unit mile = new BaseUnit("m", "mile", "length", false, false);
        Unit blackMamba = new DerivedUnit("m", "mamba", "length", new Conversion(42, DIVIDE));
        toTest.save(meter);
        assertThrows(Exception.class, () -> toTest.save(mile));
        assertThrows(Exception.class, () -> toTest.save(blackMamba));
    }

    @Test
    public void cannotSaveTwoUnitsWithTheSameNameForTheSameUnitType() {
        Unit meter = new BaseUnit("m", "meter", "length", true, true);
        Unit meterDuplicate = new BaseUnit("m", "meter", "length", true, true);
        Unit insaneMeter = new DerivedUnit("mt", "meter", "length", new Conversion(42, DIVIDE));
        toTest.save(meter);
        assertThrows(Exception.class, () -> toTest.save(meterDuplicate));
        assertThrows(Exception.class, () -> toTest.save(insaneMeter));
    }

    @Test
    public void testGetAllRetrievesAllUnitsAndGroupsThemCorrectly() {
        final BaseUnit meter = new BaseUnit("m", "meter", "length", true, true);
        toTest.save(meter);
        final Unit nauticalMile = new DerivedUnit("ml", "nautical mile", "length", new Conversion(1852, MULTIPLY));
        toTest.save(nauticalMile);
        final BaseUnit au = new BaseUnit("au", "astronomical unit", "cosmic length", false, false);
        toTest.save(au);

        final Map<String, List<UnitView>> allUnitsGrouped = toTest.getAll();
        assertEquals(2, allUnitsGrouped.size());

        final List<UnitView> lengthUnits = allUnitsGrouped.get("length");
        assertEquals(8, lengthUnits.size()); //6 auto-created, derived units, meter and nautical mile
        assertTrue(lengthUnits.contains(new BaseUnitView(meter)));
        assertTrue(lengthUnits.contains(nauticalMile));
        assertTrue(lengthUnits.contains(new DerivedUnit("km", "kilometer", "length", new Conversion(1_000, MULTIPLY))));

        final List<UnitView> cosmicUnits = allUnitsGrouped.get("cosmic length");
        assertEquals(1, cosmicUnits.size());
        assertTrue(cosmicUnits.contains(new BaseUnitView(au)));
    }
}
