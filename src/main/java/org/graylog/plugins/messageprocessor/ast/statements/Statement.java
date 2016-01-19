package org.graylog.plugins.messageprocessor.ast.statements;

import org.graylog.plugins.messageprocessor.EvaluationContext;

public interface Statement {

    // TODO should this have a return value at all?
    Object evaluate(EvaluationContext context);
}
