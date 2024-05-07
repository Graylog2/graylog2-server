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

import jakarta.inject.Inject;
import org.graylog.plugins.formatting.units.model.SupportedUnits;
import org.graylog.plugins.formatting.units.model.Unit;
import org.graylog.plugins.formatting.units.model.UnitId;

import java.util.Optional;

import static org.graylog.schema.HttpFields.HTTP_BYTES;
import static org.graylog.schema.HttpFields.HTTP_RESPONSE_BYTES;
import static org.graylog.schema.NetworkFields.NETWORK_BYTES;
import static org.graylog.schema.NetworkFields.NETWORK_DATA_BYTES;
import static org.graylog.schema.NetworkFields.NETWORK_HEADER_BYTES;
import static org.graylog.schema.SourceFields.SOURCE_BYTES_SENT;
import static org.graylog2.plugin.Message.FIELD_GL2_ACCOUNTED_MESSAGE_SIZE;
import static org.graylog2.plugin.Message.FIELD_GL2_PROCESSING_DURATION_MS;

public class HardcodedFieldUnitObtainingMethod implements FieldUnitObtainingMethod {

    private final SupportedUnits supportedUnits;


    @Inject
    public HardcodedFieldUnitObtainingMethod(final SupportedUnits supportedUnits) {
        this.supportedUnits = supportedUnits;
    }

    @Override
    public Optional<Unit> obtainUnit(final String fieldName) {
        return switch (fieldName) {
            case FIELD_GL2_PROCESSING_DURATION_MS -> supportedUnits.getUnit(new UnitId("time", "ms"));
            case FIELD_GL2_ACCOUNTED_MESSAGE_SIZE,
                    HTTP_BYTES,
                    HTTP_RESPONSE_BYTES,
                    NETWORK_BYTES,
                    NETWORK_DATA_BYTES,
                    NETWORK_HEADER_BYTES,
                    SOURCE_BYTES_SENT -> supportedUnits.getUnit(new UnitId("size", "B"));
            default -> Optional.empty();
        };

    }
}
