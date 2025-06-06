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
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record SupportedUnits(
        @JsonProperty(value = "units", required = true) Map<String, List<Unit>> unitsGroupedByType) {

    private static final Logger LOG = LoggerFactory.getLogger(SupportedUnits.class);

    @JsonIgnore
    public Optional<Unit> getUnit(final UnitId unitId) {
        final Optional<Unit> unitView = unitsGroupedByType()
                .getOrDefault(unitId.unitType(), List.of())
                .stream()
                .filter(u -> u.abbrev().equals(unitId.abbrev()))
                .findFirst();

        if (unitView.isEmpty()) {
            LOG.error("List of supported units does not contain unit " + unitId.abbrev() + " of type" + unitId.unitType() + ". Unit related functionality may function improperly");
        }
        return unitView;
    }
}
