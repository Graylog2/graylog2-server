package org.graylog.plugins.pipelineprocessor.parser.errors;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog.plugins.pipelineprocessor.parser.RuleLangParser;

public class IncompatibleIndexType extends ParseError {
    private final Class<?> expected;
    private final Class<?> actual;

    public IncompatibleIndexType(RuleLangParser.IndexedAccessContext ctx,
                                 Class<?> expected,
                                 Class<?> actual) {
        super("incompatible_index_type", ctx);
        this.expected = expected;
        this.actual = actual;
    }

    @JsonProperty("reason")
    @Override
    public String toString() {
        return "Expected type " + expected.getSimpleName() + " but found " + actual.getSimpleName() + " when indexing" + positionString();
    }
}
