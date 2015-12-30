package org.graylog.plugins.messageprocessor.parser;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UndeclaredVariable extends ParseError {

    @JsonIgnore
    private final RuleLangParser.IdentifierContext ctx;

    public UndeclaredVariable(RuleLangParser.IdentifierContext ctx) {
        super("undeclared_variable", ctx);
        this.ctx = ctx;
    }

    @JsonProperty("reason")
    @Override
    public String toString() {
        return "Undeclared variable " + ctx.Identifier().getText() + positionString();
    }

}
