package org.graylog.plugins.messageprocessor.parser;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.antlr.v4.runtime.ParserRuleContext;

public abstract class ParseError {

    @JsonIgnore
    private final ParserRuleContext ctx;

    protected ParseError(ParserRuleContext ctx) {
        this.ctx = ctx;
    }

    @JsonProperty
    public int line() {
        return ctx.getStart().getLine();
    }

    @JsonProperty
    public int positionInLine() {
        return ctx.getStart().getCharPositionInLine();
    }

    protected String positionString() {
        return " in" +
                " line " + line() +
                " pos " + positionInLine();
    }
}
