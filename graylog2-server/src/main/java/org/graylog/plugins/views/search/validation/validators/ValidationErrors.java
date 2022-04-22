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

import org.graylog.plugins.views.search.elasticsearch.QueryParam;
import org.graylog.plugins.views.search.engine.QueryPosition;
import org.graylog.plugins.views.search.errors.EmptyParameterError;
import org.graylog.plugins.views.search.errors.MissingEnterpriseLicenseException;
import org.graylog.plugins.views.search.errors.SearchException;
import org.graylog.plugins.views.search.errors.UnboundParameterError;
import org.graylog.plugins.views.search.validation.ValidationMessage;
import org.graylog.plugins.views.search.validation.ValidationStatus;
import org.graylog.plugins.views.search.validation.ValidationType;
import org.graylog2.shared.utilities.ExceptionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ValidationErrors {

    private static final Pattern regexPosition = Pattern.compile(".*at line (\\d+), column (\\d+).", Pattern.MULTILINE | Pattern.DOTALL);

    public static List<ValidationMessage> create(SearchException searchException) {
        if (searchException.error() instanceof UnboundParameterError) {
            final UnboundParameterError error = (UnboundParameterError) searchException.error();
            return paramsToValidationMessages(
                    ValidationStatus.ERROR,
                    error.allUnknownParameters(),
                    ValidationType.UNDECLARED_PARAMETER,
                    param -> "Unbound required parameter used: " + param.name()
            );
        } else if (searchException.error() instanceof EmptyParameterError) {
            final EmptyParameterError error = (EmptyParameterError) searchException.error();
            return paramsToValidationMessages(
                    ValidationStatus.WARNING,
                    Collections.singleton(error.getParameterUsage()),
                    ValidationType.EMPTY_PARAMETER,
                    param -> error.description());
        }
        return create(searchException);
    }

    public static List<ValidationMessage> create(MissingEnterpriseLicenseException licenseException) {
        return paramsToValidationMessages(
                ValidationStatus.ERROR,
                licenseException.getQueryParams(),
                ValidationType.MISSING_LICENSE,
                param -> "Search parameter used without enterprise license: " + param.name());
    }


    public static List<ValidationMessage> create(final Exception exception) {

        final String input = exception.toString();

        final ValidationMessage.Builder errorBuilder = ValidationMessage.builder(ValidationStatus.ERROR, ValidationType.QUERY_PARSING_ERROR);

        final String rootCause = getErrorMessage(exception);
        errorBuilder.errorMessage(String.format(Locale.ROOT, "Cannot parse query, cause: %s", rootCause));

        final Matcher positionMatcher = regexPosition.matcher(input);
        if (positionMatcher.find()) {
            errorBuilder.position(QueryPosition.builder()
                    .beginLine(1)
                    .beginColumn(0)
                    .endLine(Integer.parseInt(positionMatcher.group(1)))
                    .endColumn(Integer.parseInt(positionMatcher.group(2)))
                    .build());
        }

        return Collections.singletonList(errorBuilder.build());
    }

    private static String getErrorMessage(Exception exception) {
        final String rootCause = ExceptionUtils.getRootCauseMessage(exception);

        if (rootCause.contains("Encountered \"<EOF>\"")) {
            return "incomplete query, query ended unexpectedly";
        }

        return rootCause;
    }

    private static List<ValidationMessage> paramsToValidationMessages(ValidationStatus validationStatus, final Set<QueryParam> params, final ValidationType errorType, final Function<QueryParam, String> messageBuilder) {
        return params.stream()
                .flatMap(param -> {
                    final String errorMessage = messageBuilder.apply(param);
                    return param.positions()
                            .stream()
                            .map(pos -> ValidationMessage.builder(validationStatus, errorType)
                                    .errorMessage(errorMessage)
                                    .position(QueryPosition.from(pos))
                                    .relatedProperty(param.name())
                                    .build()
                            );
                })
                .collect(Collectors.toList());
    }
}
