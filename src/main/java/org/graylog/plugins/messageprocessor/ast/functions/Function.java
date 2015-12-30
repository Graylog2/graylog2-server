package org.graylog.plugins.messageprocessor.ast.functions;

import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog.plugins.messageprocessor.ast.expressions.Expression;
import org.graylog2.plugin.Message;

import java.util.Map;

public interface Function {

    Object evaluate(Map<String, Expression> args, EvaluationContext context, Message message);

    FunctionDescriptor descriptor();

}
