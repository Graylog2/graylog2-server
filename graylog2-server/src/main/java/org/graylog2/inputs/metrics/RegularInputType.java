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
package org.graylog2.inputs.metrics;

import jakarta.inject.Inject;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog2.shared.security.RestPermissions;

import java.util.Set;
import java.util.stream.Collectors;

public class RegularInputType implements InputType {
    public static final String NAME = "input";

    private final InputService inputService;

    @Inject
    public RegularInputType(InputService inputService) {
        this.inputService = inputService;
    }

    @Override
    public String typeName() {
        return NAME;
    }

    @Override
    public String readPermission() {
        return RestPermissions.INPUTS_READ;
    }

    @Override
    public Set<String> filterMembers(Set<String> candidateIds) {
        return inputService.findByIds(candidateIds).stream()
                .map(Input::getId)
                .collect(Collectors.toSet());
    }
}
