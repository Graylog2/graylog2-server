package org.graylog.plugins.messageprocessor.ast.statements;

import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog2.plugin.Message;

public interface Statement {

    Object evaluate(EvaluationContext context, Message message);
}
