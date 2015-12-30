package org.graylog.plugins.messageprocessor.parser;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UndeclaredFunction extends ParseError {
    private final RuleLangParser.FunctionCallContext ctx;

    public UndeclaredFunction(RuleLangParser.FunctionCallContext ctx) {
        super("undeclared_function", ctx);
        this.ctx = ctx;
    }

    @JsonProperty("reason")
    @Override
    public String toString() {
        return "Unknown function " + ctx.funcName.getText() + positionString();
    }
}
