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
package org.graylog.plugins.views.search.elasticsearch;

import org.graylog.plugins.views.search.ParameterProvider;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.engine.QueryStringDecorator;
import org.graylog.plugins.views.search.errors.EmptyParameterError;
import org.graylog.plugins.views.search.errors.MissingEnterpriseLicenseException;
import org.graylog.plugins.views.search.errors.SearchException;
import org.graylog.plugins.views.search.errors.UnboundParameterError;
import org.graylog.plugins.views.search.validation.QueryValidationService;
import org.graylog.plugins.views.search.validation.ValidationMessage;
import org.graylog.plugins.views.search.validation.ValidationRequest;
import org.graylog.plugins.views.search.validation.ValidationResponse;
import org.graylog.plugins.views.search.validation.ValidationType;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class QueryStringDecorators implements QueryStringDecorator, QueryValidationService {
    private final Set<QueryStringDecorator> queryDecorators;

    public static class Fake extends QueryStringDecorators {
        public Fake() {
            super(Collections.emptySet());
        }
    }

    @Inject
    public QueryStringDecorators(Set<QueryStringDecorator> queryDecorators) {
        this.queryDecorators = queryDecorators;
    }

    @Override
    public String decorate(String queryString, ParameterProvider job, Query query) {
        return this.queryDecorators.isEmpty() ? queryString : this.queryDecorators.stream()
                .reduce(queryString, (prev, decorator) -> decorator.decorate(prev, job, query), String::concat);
    }

    @Override
    public ValidationResponse validate(final ValidationRequest req) {
        //TODO: instead of centralized exception handling we have to switch to proper validation in each QueryStringDecorator (effectively : QueryStringParameterSubstitution)
        try {
            ParameterProvider parameterProvider = (name) -> req.parameters().stream().filter(p -> Objects.equals(p.name(), name)).findFirst();
            final Query query = Query.builder().query(req.query()).timerange(req.timerange()).build();
            this.decorate(req.getCombinedQueryWithFilter(), parameterProvider, query);
        } catch (SearchException searchException) {
            return convert(searchException);
        } catch (MissingEnterpriseLicenseException licenseException) {
            return ValidationResponse.error(paramsToValidationErrors(
                            licenseException.getQueryParams(),
                            ValidationType.MISSING_LICENSE,
                            param -> "Search parameter used without enterprise license: " + param.name()
                    )
            );
        }
        return ValidationResponse.ok();
    }

    private ValidationResponse convert(SearchException searchException) {
        if (searchException.error() instanceof UnboundParameterError) {
            final UnboundParameterError error = (UnboundParameterError) searchException.error();
            return ValidationResponse.error(paramsToValidationErrors(
                    error.allUnknownParameters(),
                    ValidationType.UNDECLARED_PARAMETER,
                    param -> "Unbound required parameter used: " + param.name()
            ));
        } else if (searchException.error() instanceof EmptyParameterError) {
            final EmptyParameterError error = (EmptyParameterError) searchException.error();
            return ValidationResponse.warning(paramsToValidationErrors(
                    Collections.singleton(error.getParameterUsage()),
                    ValidationType.EMPTY_PARAMETER,
                    param -> error.description()));
        }
        return ValidationResponse.error(Collections.singletonList(ValidationMessage.fromException(searchException)));
    }

    private List<ValidationMessage> paramsToValidationErrors(final Set<QueryParam> params, final ValidationType errorType, final Function<QueryParam, String> messageBuilder) {
        return params.stream()
                .flatMap(param -> {
                    final String errorMessage = messageBuilder.apply(param);
                    return param.positions()
                            .stream()
                            .map(p -> ValidationMessage.builder(errorType)
                                    .errorMessage(errorMessage)
                                    .beginLine(p.line())
                                    .endLine(p.line())
                                    .beginColumn(p.beginColumn())
                                    .endColumn(p.endColumn())
                                    .relatedProperty(param.name())
                                    .build()
                            );
                })
                .collect(Collectors.toList());
    }
}
