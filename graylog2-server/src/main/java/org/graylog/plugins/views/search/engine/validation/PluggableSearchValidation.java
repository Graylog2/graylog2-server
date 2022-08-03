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
package org.graylog.plugins.views.search.engine.validation;

import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchExecutionGuard;
import org.graylog.plugins.views.search.errors.SearchError;
import org.graylog.plugins.views.search.permissions.StreamPermissions;

import javax.inject.Inject;
import java.util.Set;
import java.util.stream.Collectors;

public class PluggableSearchValidation implements SearchValidation {
    private final SearchExecutionGuard executionGuard;
    private final Set<SearchValidator> pluggableSearchValidators;

    @Inject
    public PluggableSearchValidation(SearchExecutionGuard executionGuard,
                                     Set<SearchValidator> pluggableSearchValidators) {
        this.executionGuard = executionGuard;
        this.pluggableSearchValidators = pluggableSearchValidators;
    }

    public Set<SearchError> validate(Search search, StreamPermissions streamPermissions) {
        this.executionGuard.check(search, streamPermissions::canReadStream);

        return this.pluggableSearchValidators.stream()
                .flatMap(validator -> validator.validate(search, streamPermissions).stream())
                .collect(Collectors.toSet());
    }
}
