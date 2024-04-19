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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import jakarta.inject.Inject;
import org.graylog.plugins.formatting.units.model.BaseUnit;
import org.graylog.plugins.formatting.units.model.Conversion;
import org.graylog.plugins.formatting.units.model.DerivedUnit;
import org.graylog.plugins.formatting.units.model.Unit;
import org.graylog.plugins.formatting.units.model.UnitView;
import org.graylog2.database.MongoCollections;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.graylog.plugins.formatting.units.model.Conversion.ConversionAction.DIVIDE;
import static org.graylog.plugins.formatting.units.model.Conversion.ConversionAction.MULTIPLY;


public class UnitService {

    static final String UNITS_COLL_NAME = "units";

    private final MongoCollection<Unit> unitMongoCollection;

    @Inject
    public UnitService(final MongoCollections mongoCollections) {
        unitMongoCollection = mongoCollections.get(UNITS_COLL_NAME, Unit.class);
        unitMongoCollection.createIndex(Indexes.ascending(Unit.UNIT_TYPE, Unit.NAME), new IndexOptions().unique(true));
        unitMongoCollection.createIndex(Indexes.ascending(Unit.UNIT_TYPE, Unit.ABBREVIATION), new IndexOptions().unique(true));
    }

    public void save(final Unit unit) {
        unitMongoCollection.insertOne(unit);
    }

    public Map<String, List<UnitView>> getAll() {
        Map<String, List<UnitView>> groupedByUnitType = new HashMap<>();
        unitMongoCollection.find().forEach(unit -> {
            groupedByUnitType.putIfAbsent(unit.unitType(), new LinkedList<>());
            final List<UnitView> unitsOfTheSameUnitType = groupedByUnitType.get(unit.unitType());

            addDownScaleUnitsIfNeeded(unit, unitsOfTheSameUnitType);
            unitsOfTheSameUnitType.add(unit.asUnitView());
            addUpScaleUnitsIfNeeded(unit, unitsOfTheSameUnitType);
        });

        return groupedByUnitType;
    }

    private void addUpScaleUnitsIfNeeded(final Unit unit,
                                         List<UnitView> unitsOfTheSameUnitType) {
        if (unit instanceof final BaseUnit baseUnit) {
            if (baseUnit.generateCommonUpScaleUnits()) {
                unitsOfTheSameUnitType.add(derivedMultiplyingUnitFrom(baseUnit, "k", "kilo", 1_000));
                unitsOfTheSameUnitType.add(derivedMultiplyingUnitFrom(baseUnit, "M", "mega", 1_000_000));
                unitsOfTheSameUnitType.add(derivedMultiplyingUnitFrom(baseUnit, "G", "giga", 1_000_000_000));
            }
        }
    }

    private void addDownScaleUnitsIfNeeded(final Unit unit,
                                           List<UnitView> unitsOfTheSameUnitType) {
        if (unit instanceof final BaseUnit baseUnit) {
            if (baseUnit.generateCommonDownScaleUnits()) {
                unitsOfTheSameUnitType.add(derivedDividingUnitFrom(baseUnit, "n", "nano", 1_000_000_000));
                unitsOfTheSameUnitType.add(derivedDividingUnitFrom(baseUnit, "μ", "micro", 1_000_000));
                unitsOfTheSameUnitType.add(derivedDividingUnitFrom(baseUnit, "m", "milli", 1_000));
            }
        }
    }

    private DerivedUnit derivedDividingUnitFrom(final BaseUnit baseUnit,
                                                final String abbrevPrefix,
                                                final String namePrefix,
                                                final long divider) {
        return derivedDividingUnitFrom(baseUnit, abbrevPrefix, namePrefix, divider, DIVIDE);
    }

    private DerivedUnit derivedMultiplyingUnitFrom(final BaseUnit baseUnit,
                                                   final String abbrevPrefix,
                                                   final String namePrefix,
                                                   final long multiplier) {
        return derivedDividingUnitFrom(baseUnit, abbrevPrefix, namePrefix, multiplier, MULTIPLY);
    }

    private DerivedUnit derivedDividingUnitFrom(final BaseUnit baseUnit,
                                                final String abbrevPrefix,
                                                final String namePrefix,
                                                final long value,
                                                final Conversion.ConversionAction action
    ) {
        return new DerivedUnit(
                abbrevPrefix + baseUnit.abbrev(),
                namePrefix + baseUnit.name(),
                baseUnit.unitType(),
                new Conversion(value, action)
        );
    }
}
