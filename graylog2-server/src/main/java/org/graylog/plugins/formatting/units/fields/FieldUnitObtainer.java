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
import org.graylog.plugins.formatting.units.model.Unit;

import java.util.List;
import java.util.Optional;

public class FieldUnitObtainer {

    private final List<FieldUnitObtainingMethod> prioritizedMethodList;

    @Inject
    public FieldUnitObtainer(final HardcodedFieldUnitObtainingMethod hardcodedFieldUnitObtainingMethod) {
        prioritizedMethodList = List.of(hardcodedFieldUnitObtainingMethod);
    }

    public Optional<Unit> obtainUnit(final String fieldName) {
        return prioritizedMethodList.stream()
                .map(fieldUnitObtainingMethod -> fieldUnitObtainingMethod.obtainUnit(fieldName))
                .filter(Optional::isPresent)
                .findFirst()
                .orElse(Optional.empty());
    }
}
