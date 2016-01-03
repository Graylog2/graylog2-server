package org.graylog.plugins.messageprocessor.parser;

import org.graylog.plugins.messageprocessor.parser.errors.ParseError;

import java.util.Set;

public class ParseException extends RuntimeException {
    private final Set<ParseError> errors;

    public ParseException(Set<ParseError> errors) {
        this.errors = errors;
    }

    public Set<ParseError> getErrors() {
        return errors;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder("Errors:\n");
        for (ParseError parseError : getErrors()) {
            sb.append(" ").append(parseError).append("\n");
        }
        return sb.toString();
    }
}
