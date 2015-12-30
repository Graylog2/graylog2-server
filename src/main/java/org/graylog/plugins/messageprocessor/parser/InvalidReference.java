package org.graylog.plugins.messageprocessor.parser;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class InvalidReference implements ParseError {
    @JsonIgnore
    private final RuleLangParser.IdentifierContext ctx;

    public InvalidReference(RuleLangParser.IdentifierContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String toString() {
        return "Undeclared variable " + ctx.Identifier().getText() + " in" +
                " line " + ctx.getStart().getLine() +
                " pos " + ctx.getStart().getCharPositionInLine();
    }

    @JsonProperty
    public int line() {
        return ctx.getStart().getLine();
    }

    @JsonProperty
    public int positionInLine() {
        return ctx.getStart().getCharPositionInLine();
    }
}
