package org.graylog.plugins.pipelineprocessor.parser.errors;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog.plugins.pipelineprocessor.parser.RuleLangParser;

public class NonIndexableType extends ParseError {
    private final Class<?> indexableType;

    public NonIndexableType(RuleLangParser.IndexedAccessContext ctx, Class<?> indexableType) {
        super("non_indexable", ctx);
        this.indexableType = indexableType;
    }

    @JsonProperty("reason")
    @Override
    public String toString() {
        return "Cannot index value of type " + indexableType.getSimpleName() + positionString();
    }
}
