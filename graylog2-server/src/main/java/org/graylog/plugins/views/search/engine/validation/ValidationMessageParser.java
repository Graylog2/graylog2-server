package org.graylog.plugins.views.search.engine.validation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValidationMessageParser {

    private static final Pattern regexFull = Pattern.compile("(?:\\[\\w+/\\w+]\\s+)*(\\w+)\\[(.*?)];\\s+nested:.*",  Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern regexSimple = Pattern.compile("([\\w.]+):\\s+(.*)",  Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern regexPosition = Pattern.compile(".*at line (\\d+), column (\\d+).",  Pattern.MULTILINE | Pattern.DOTALL);


    public static ValidationMessage getHumanReadableMessage(final String input) {

        if (input == null) {
            return ValidationMessage.builder().build();
        }

        final ValidationMessage.Builder errorBuilder = ValidationMessage.builder();

        final Matcher matcher = regexFull.matcher(input);
        if (matcher.find()) {
            errorBuilder.errorType(matcher.group(1));
            errorBuilder.errorMessage(matcher.group(2));
        } else {
            final Matcher simpleMatcher = regexSimple.matcher(input);
            if (simpleMatcher.find()) {
                errorBuilder.errorType(simpleMatcher.group(1));
                errorBuilder.errorMessage(simpleMatcher.group(2));
            } else {
                errorBuilder.errorMessage(input);
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
