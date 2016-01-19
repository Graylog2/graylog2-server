package org.graylog.plugins.messageprocessor.parser.errors;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog.plugins.messageprocessor.parser.RuleLangParser;

public class IncompatibleType extends ParseError {
    private final Class<?> expected;
    private final Class<?> actual;

    public IncompatibleType(RuleLangParser.MessageRefContext ctx, Class<?> expected, Class<?> actual) {
        super("incompatible_type", ctx);
        this.expected = expected;
        this.actual = actual;
    }

    @JsonProperty("reason")
    @Override
    public String toString() {
        return "Expected type " + expected.getSimpleName() + " but found " + actual.getSimpleName() + positionString();
    }
}
