package org.graylog.plugins.messageprocessor.parser;

import java.util.List;

public class ParseException extends Throwable {
    private final List<ParseError> errors;

    public ParseException(List<ParseError> errors) {
        this.errors = errors;
    }

    public List<ParseError> getErrors() {
        return errors;
    }
}
