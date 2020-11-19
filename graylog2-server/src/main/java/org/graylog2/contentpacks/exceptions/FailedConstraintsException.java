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
package org.graylog2.contentpacks.exceptions;

import org.graylog2.contentpacks.model.constraints.Constraint;

import java.util.Collection;

public class FailedConstraintsException extends ContentPackException {
    private final Collection<Constraint> failedConstraints;

    public FailedConstraintsException(Collection<Constraint> failedConstraints) {
        super("Failed constraints: " + failedConstraints);
        this.failedConstraints = failedConstraints;
    }

    public Collection<Constraint> getFailedConstraints() {
        return failedConstraints;
    }
}
