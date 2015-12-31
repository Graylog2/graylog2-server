package org.graylog.plugins.messageprocessor.parser;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.Objects;

public abstract class ParseError {

    @JsonProperty
    private final String type;

    @JsonIgnore
    private final ParserRuleContext ctx;

    protected ParseError(String type, ParserRuleContext ctx) {
        this.type = type;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParseError)) return false;
        ParseError that = (ParseError) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(ctx, that.ctx);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, ctx);
    }
}
