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
package org.graylog.plugins.formatting.units.migrations;

import jakarta.inject.Inject;
import org.graylog.plugins.formatting.units.db.UnitService;
import org.graylog.plugins.formatting.units.model.BaseUnit;
import org.graylog.plugins.formatting.units.model.Conversion;
import org.graylog.plugins.formatting.units.model.DerivedUnit;
import org.graylog.plugins.formatting.units.model.Unit;
import org.graylog2.migrations.Migration;

import java.time.ZonedDateTime;

import static org.graylog.plugins.formatting.units.model.Conversion.ConversionAction.MULTIPLY;

public class V20240422120000_SetInitialUnits extends Migration {

    private final UnitService unitService;

    @Inject
    public V20240422120000_SetInitialUnits(final UnitService unitService) {
        this.unitService = unitService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2024-04-22T12:00:00Z");
    }

    @Override
    public void upgrade() {
        if (unitService.getAll().isEmpty()) {
            insertSizeUnits();
            insertTimeUnits();
            insertPercentageUnits();
        }
    }

    private void insertSizeUnits() {
        Unit bajt = new BaseUnit("b", "byte", "size", true, false);
        unitService.save(bajt);
    }

    private void insertTimeUnits() {
        Unit second = new BaseUnit("s", "second", "time", false, true);
        unitService.save(second);

        Unit minute = new DerivedUnit("min", "minute", "time", new Conversion(60, MULTIPLY));
        unitService.save(minute);
        Unit hour = new DerivedUnit("h", "hour", "time", new Conversion(60 * 60, MULTIPLY));
        unitService.save(hour);
        Unit day = new DerivedUnit("d", "day", "time", new Conversion(24 * 60 * 60, MULTIPLY));
        unitService.save(day);
        Unit month = new DerivedUnit("m", "month", "time", new Conversion(30 * 24 * 60 * 60, MULTIPLY));
        unitService.save(month);
    }

    private void insertPercentageUnits() {
        Unit percent = new BaseUnit("%", "percent", "percent", false, false);
        unitService.save(percent);
    }
}
