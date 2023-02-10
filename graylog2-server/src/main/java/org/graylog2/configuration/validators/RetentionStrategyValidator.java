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
import org.graylog2.indexer.retention.strategies.ClosingRetentionStrategy;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy;
import org.graylog2.indexer.retention.strategies.NoopRetentionStrategy;

import java.util.Set;
import java.util.stream.Collectors;

public class RetentionStrategyValidator implements Validator<Set<String>> {
    private static final String ARCHIVE_RETENTION_STRATEGY = "archive";
    Set<String> VALID_STRATEGIES = Sets.newHashSet(
            NoopRetentionStrategy.NAME, ClosingRetentionStrategy.NAME, DeletionRetentionStrategy.NAME);

    @Override
    // The set of valid retention strategies must
    // - contain only names of supported strategies
    // - at least one must stay enabled
    public void validate(String parameter, Set<String> values) throws ValidationException {

        if (!values.stream()
                .filter(s -> !VALID_STRATEGIES.contains(s) && !ARCHIVE_RETENTION_STRATEGY.equals(s))
                .collect(Collectors.toSet()).isEmpty()) {
            throw new ValidationException("Parameter " + parameter + " contains invalid values: " + values);
        }

        if (values.containsAll(VALID_STRATEGIES)) {
            throw new ValidationException(parameter + ":" + values + " At least one retention of the following [none, close, delete], should stay enabled!");
        }
    }
}
