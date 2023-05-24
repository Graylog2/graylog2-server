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
package org.graylog.plugins.views.search.rest.scriptingapi.mapping;

import org.apache.commons.collections.CollectionUtils;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.errors.SearchError;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.scriptingapi.request.RequestedField;
import org.graylog.plugins.views.search.rest.scriptingapi.response.decorators.FieldDecorator;
import org.graylog2.shared.utilities.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public interface TabularResponseCreator {

    default void throwErrorIfAnyAvailable(final QueryResult queryResult) throws QueryFailedException {
        if (!CollectionUtils.isEmpty(queryResult.errors())) {
            final String errorText = queryResult.errors().stream().map(SearchError::description).collect(Collectors.joining(", "));
            throw new QueryFailedException("Failed to obtain results. " + errorText);
        }
    }

    default Object decorate(Set<FieldDecorator> decorators, RequestedField field, Object val, SearchUser searchUser) {
        final List<Object> decorated = decorators.stream()
                .filter(d -> d.accept(field))
                .map(d -> d.decorate(field, val, searchUser))
                .toList();

        checkDecoratorErrors(field, decorated);

        return decorated.stream().findFirst().orElse(val);
    }

    default void checkDecoratorErrors(RequestedField field, List<Object> decorated) {
        // we tried to decorate, some decorator is defined but no value returned
        if (decorated.isEmpty() && field.hasDecorator()) {
            throw new UnsupportedOperationException(StringUtils.f("Unsupported property '%s' on field '%s'", field.decorator(), field.name()));
        }

        // more decorators deliver a value, we don't know what's correct. This is not supported case.
        if (decorated.size() > 1) {
            throw new UnsupportedOperationException(
                    StringUtils.f("Found more decorators supporting '%s' on field '%s', this is not supported operation.", field.decorator(), field.name())
            );
        }
    }
}
