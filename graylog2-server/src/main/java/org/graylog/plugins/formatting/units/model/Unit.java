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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Unit, stored in MongoDB.
 */
@JsonSubTypes({
        @JsonSubTypes.Type(value = BaseUnit.class),
        @JsonSubTypes.Type(value = DerivedUnit.class),
})
@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
public interface Unit {

    String UNIT_TYPE = "unit_type";
    String ABBREVIATION = "abbrev";
    String NAME = "name";

    String abbrev();

    String name();

    String unitType();

    @JsonIgnore
    UnitView asUnitView();

}
