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

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@JsonInclude(JsonInclude.Include.NON_NULL)
@AutoValue
public abstract class ValidationMessage {

    private static final Pattern regexSimple = Pattern.compile("([\\w.]+):\\s+(.*)", Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern regexPosition = Pattern.compile(".*at line (\\d+), column (\\d+).", Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern regexForExplanation = Pattern.compile("(\\w+)\\(\"(.+)\"\\)", Pattern.MULTILINE | Pattern.DOTALL);

    @JsonProperty
    @Nullable
    public abstract Integer beginLine();

    @JsonProperty
    @Nullable
    public abstract Integer beginColumn();


    @JsonProperty
    @Nullable
    public abstract Integer endLine();

    @JsonProperty
    @Nullable
    public abstract Integer endColumn();

    @JsonProperty
    @Nullable
    public abstract String errorType();

    @JsonProperty
    public abstract String errorMessage();

    public static ValidationMessage fromException(final String query, final Exception exception) {

        final String input = exception.toString();

        final ValidationMessage.Builder errorBuilder = builder();

        final Matcher simpleMatcher = regexSimple.matcher(input);
        if (simpleMatcher.find()) {
            final String fullyQualifiedName = simpleMatcher.group(1);
            final String[] parts = fullyQualifiedName.split("\\.");
            if (parts.length > 0) {
                errorBuilder.errorType(parts[parts.length - 1]);
            } else {
                errorBuilder.errorType(fullyQualifiedName);
            }
            errorBuilder.errorMessage(simpleMatcher.group(2));
        }

        final Matcher explanationMatcher = regexForExplanation.matcher(input);
        if (explanationMatcher.find()) {
            errorBuilder.errorType(explanationMatcher.group(1));
            errorBuilder.errorMessage(explanationMatcher.group(2));
        }


        final Matcher positionMatcher = regexPosition.matcher(input);
        if (positionMatcher.find()) {
            errorBuilder.beginLine(1);
            errorBuilder.beginColumn(0);
            // errorBuilder.beginLine(Integer.parseInt(positionMatcher.group(1)));
            // errorBuilder.beginColumn(Integer.parseInt(positionMatcher.group(2)));
            errorBuilder.endColumn(query.length());
            errorBuilder.endLine(countLines(query));
        }

        // Fallback, all parsing failed
        if (!errorBuilder.errorMessage().isPresent()) {
            errorBuilder.errorMessage(input);
        }

        return errorBuilder.build();
    }

    private static int countLines(String str){
        String[] lines = str.split("\r\n|\r|\n");
        return  lines.length;
    }


    public static Builder builder() {
        return new AutoValue_ValidationMessage.Builder();
    }


    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder beginLine(int line);

        public abstract Builder beginColumn(int column);

        public abstract Builder endLine(int line);

        public abstract Builder endColumn(int column);

        public abstract Builder errorType(@Nullable String errorType);

        public abstract Builder errorMessage(String errorMessage);

        public abstract Optional<String> errorMessage();

        public abstract ValidationMessage build();

    }


}
