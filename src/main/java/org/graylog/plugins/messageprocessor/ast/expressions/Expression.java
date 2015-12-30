package org.graylog.plugins.messageprocessor.ast.expressions;

import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog2.plugin.Message;

public interface Expression<T> {
    T evaluate(EvaluationContext context, Message message);
}
