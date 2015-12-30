package org.graylog.plugins.messageprocessor.parser;

public class UndeclaredFunction extends ParseError {
    private final RuleLangParser.FunctionCallContext ctx;

    public UndeclaredFunction(RuleLangParser.FunctionCallContext ctx) {
        super(ctx);
        this.ctx = ctx;
    }

    @Override
    public String toString() {
        return "Unknown function " + ctx.funcName.getText() + positionString();
    }
}
