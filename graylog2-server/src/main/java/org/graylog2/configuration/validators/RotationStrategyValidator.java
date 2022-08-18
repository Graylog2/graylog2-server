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
package org.graylog2.configuration.validators;

import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.Validator;
import com.google.common.collect.Sets;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.strategies.SizeBasedRotationStrategy;
import org.graylog2.indexer.rotation.strategies.TimeBasedRotationStrategy;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RotationStrategyValidator implements Validator<List<String>> {
    Set<String> VALID_STRATEGIES = Sets.newHashSet(
            MessageCountRotationStrategy.NAME, SizeBasedRotationStrategy.NAME, TimeBasedRotationStrategy.NAME);

    @Override
    // The set of valid rotation strategies must
    // - contain only names of supported strategies
    // - not be empty
    public void validate(String parameter, List<String> value) throws ValidationException {
        if (value == null || value.isEmpty()) {
            throw new ValidationException("Parameter " + parameter + " should be non-empty list");
        }

        if (!value.stream()
                .filter(s -> !VALID_STRATEGIES.contains(s))
                .collect(Collectors.toSet()).isEmpty()) {
            throw new ValidationException("Parameter " + parameter + " contains invalid values: " + value);
        }

        if (value.stream().distinct().count() != value.size()) {
            throw new ValidationException("Parameter " + parameter + " contains duplicate values: " + value);
        }
    }
}
