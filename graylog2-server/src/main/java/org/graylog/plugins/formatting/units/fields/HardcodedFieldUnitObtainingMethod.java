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
package org.graylog.plugins.formatting.units.fields;

import org.graylog.plugins.formatting.units.model.BaseUnitView;
import org.graylog.plugins.formatting.units.model.Conversion;
import org.graylog.plugins.formatting.units.model.DerivedUnit;
import org.graylog.plugins.formatting.units.model.UnitView;

import java.util.Optional;

import static org.graylog.plugins.formatting.units.model.Conversion.ConversionAction.DIVIDE;
import static org.graylog2.plugin.Message.FIELD_GL2_ACCOUNTED_MESSAGE_SIZE;
import static org.graylog2.plugin.Message.FIELD_GL2_PROCESSING_DURATION_MS;

public class HardcodedFieldUnitObtainingMethod implements FieldUnitObtainingMethod {

    @Override
    public Optional<UnitView> obtainUnit(final String fieldName) {
        //TODO: complete list
        //TODO: take units from DB instead of creating from scratch
        return switch (fieldName) {
            case FIELD_GL2_PROCESSING_DURATION_MS ->
                    Optional.of(new DerivedUnit("ms", "millisecond", "time", new Conversion(1000, DIVIDE)));
            case FIELD_GL2_ACCOUNTED_MESSAGE_SIZE -> Optional.of(new BaseUnitView("b", "byte", "size"));
            default -> Optional.empty();
        };

    }
}
