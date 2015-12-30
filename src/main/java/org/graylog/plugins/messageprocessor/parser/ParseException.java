package org.graylog.plugins.messageprocessor.parser;

import java.util.List;

public class ParseException extends RuntimeException {
    private final List<ParseError> errors;

    public ParseException(List<ParseError> errors) {
        this.errors = errors;
    }

    public List<ParseError> getErrors() {
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
