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
package org.graylog.plugins.views.search.validation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.apache.lucene.queryparser.classic.ParseException;
import org.graylog2.shared.utilities.ExceptionUtils;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AutoValue
public abstract class ValidationMessage {

    private static final Pattern regexPosition = Pattern.compile(".*at line (\\d+), column (\\d+).", Pattern.MULTILINE | Pattern.DOTALL);

    @Nullable
    public abstract Integer beginLine();

    @Nullable
    public abstract Integer beginColumn();

    @Nullable
    public abstract Integer endLine();

    @Nullable
    public abstract Integer endColumn();

    public abstract String errorMessage();

    @Nullable
    public abstract String relatedProperty();

    public abstract ValidationStatus validationStatus();

    public abstract ValidationType validationType();

    public static ValidationMessage fromException(final Exception exception) {

        final String input = exception.toString();

        final ValidationMessage.Builder errorBuilder = builder(ValidationStatus.ERROR, ValidationType.QUERY_PARSING_ERROR);

        final String rootCause = getErrorMessage(exception);
        errorBuilder.errorMessage(String.format(Locale.ROOT, "Cannot parse query, cause: %s", rootCause));

        final Matcher positionMatcher = regexPosition.matcher(input);
        if (positionMatcher.find()) {
            errorBuilder.beginLine(1);
            errorBuilder.beginColumn(0);

            errorBuilder.endLine(Integer.parseInt(positionMatcher.group(1)));
            errorBuilder.endColumn(Integer.parseInt(positionMatcher.group(2)));
        }

        return errorBuilder.build();
    }

    private static String getErrorMessage(Exception exception) {
        final String rootCause = ExceptionUtils.getRootCauseMessage(exception);

        if (rootCause.contains("Encountered \"<EOF>\"")) {
            return "incomplete query, query ended unexpectedly";
        }

        return rootCause;
    }

    public static Builder builder(ValidationStatus status, ValidationType validationType) {
        return new AutoValue_ValidationMessage.Builder()
                .validationStatus(status)
                .validationType(validationType);
    }


    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder beginLine(int line);

        public abstract Builder beginColumn(int column);

        public abstract Builder endLine(int line);

        public abstract Builder endColumn(int column);

        public abstract Builder errorMessage(String errorMessage);

        public abstract Builder relatedProperty(String relatedProperty);

        public abstract Builder validationType(ValidationType validationType);

        public abstract Builder validationStatus(ValidationStatus validationStatus);

        public abstract ValidationMessage build();
    }


}
