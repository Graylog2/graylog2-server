package org.graylog.plugins.messageprocessor.parser;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class UndeclaredVariable extends ParseError {
    @JsonIgnore
    private final RuleLangParser.IdentifierContext ctx;

    public UndeclaredVariable(RuleLangParser.IdentifierContext ctx) {
        super(ctx);
        this.ctx = ctx;
    }

    @Override
    public String toString() {
        return "Undeclared variable " + ctx.Identifier().getText() + positionString();
    }

}
