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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValidationMessageParser {

    private static final Pattern regexForNested = Pattern.compile("(\\w+)\\[(.*)]",  Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern regexSimple = Pattern.compile("([\\w.]+):\\s+(.*)",  Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern regexPosition = Pattern.compile(".*at line (\\d+), column (\\d+).",  Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern regexForExplanation = Pattern.compile("(\\w+)\\(\"(.+)\"\\)",  Pattern.MULTILINE | Pattern.DOTALL);


    public static ValidationMessage getHumanReadableMessage(final String input) {

        if (input == null) {
            return ValidationMessage.builder().build();
        }

        final ValidationMessage.Builder errorBuilder = ValidationMessage.builder();

        if(input.contains("; nested:")) {
            final String[] stack = input.split("; nested:");
            final String actualProblem = stack[1];
            final Matcher matcher = regexForNested.matcher(actualProblem);
            if(matcher.find()) {
                errorBuilder.errorType(matcher.group(1));
                errorBuilder.errorMessage(matcher.group(2));
            }
        } else {
            final Matcher simpleMatcher = regexSimple.matcher(input);
            if(simpleMatcher.find()) {
                errorBuilder.errorType(simpleMatcher.group(1));
                errorBuilder.errorMessage(simpleMatcher.group(2));
            }

            final Matcher explanationMatcher = regexForExplanation.matcher(input);
            if(explanationMatcher.find()) {
                errorBuilder.errorType(explanationMatcher.group(1));
                errorBuilder.errorMessage(explanationMatcher.group(2));
            }
        }

        final Matcher positionMatcher = regexPosition.matcher(input);
        if (positionMatcher.find()) {
            errorBuilder.line(Integer.parseInt(positionMatcher.group(1)));
            errorBuilder.column(Integer.parseInt(positionMatcher.group(2)));
        }

        return errorBuilder.build();
    }
}
