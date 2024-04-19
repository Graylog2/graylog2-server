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
package org.graylog.plugins.formatting.units.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import static org.graylog.plugins.formatting.units.model.Unit.ABBREVIATION;
import static org.graylog.plugins.formatting.units.model.Unit.NAME;
import static org.graylog.plugins.formatting.units.model.Unit.UNIT_TYPE;


/**
 * {@link BaseUnit} has some inner configuration fields, like `generateCommon...Units` family of properties, that no one needs to know about.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(BaseUnitView.TYPE)
public record BaseUnitView(@JsonProperty(value = ABBREVIATION, required = true) String abbrev,
                           @JsonProperty(value = NAME, required = true) String name,
                           @JsonProperty(value = UNIT_TYPE, required = true) String unitType) implements UnitView {

    public static final String TYPE = "base";

    public BaseUnitView(final BaseUnit baseUnit) {
        this(baseUnit.abbrev(), baseUnit.name(), baseUnit.unitType());
    }
}
