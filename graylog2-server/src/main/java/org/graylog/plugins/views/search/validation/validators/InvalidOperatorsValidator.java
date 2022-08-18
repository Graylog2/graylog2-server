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
package org.graylog.plugins.views.search.validation.validators;

import org.apache.lucene.queryparser.classic.QueryParserConstants;
import org.graylog.plugins.views.search.engine.QueryPosition;
import org.graylog.plugins.views.search.validation.ImmutableToken;
import org.graylog.plugins.views.search.validation.QueryValidator;
import org.graylog.plugins.views.search.validation.ValidationContext;
import org.graylog.plugins.views.search.validation.ValidationMessage;
import org.graylog.plugins.views.search.validation.ValidationStatus;
import org.graylog.plugins.views.search.validation.ValidationType;

import javax.inject.Singleton;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class InvalidOperatorsValidator implements QueryValidator {

    private static final Set<String> INVALID_TOKENS = new HashSet<>(
            Arrays.asList("and", "or", "not")
    );

    @Override
    public List<ValidationMessage> validate(ValidationContext context) {
        return context.query().tokens().stream()
                .filter(this::isInvalidOperator)
                .map(token -> {
                    final String errorMessage = String.format(Locale.ROOT, "Query contains invalid operator \"%s\". All AND / OR / NOT operators have to be written uppercase", token.image());
                    return ValidationMessage.builder(ValidationStatus.WARNING, ValidationType.INVALID_OPERATOR)
                            .errorMessage(errorMessage)
                            .relatedProperty(token.image())
                            .position(QueryPosition.from(token))
                            .build();
                }).collect(Collectors.toList());
    }

    private boolean isInvalidOperator(ImmutableToken token) {
        return token.kind() == QueryParserConstants.TERM && INVALID_TOKENS.contains(token.image());
    }
}
