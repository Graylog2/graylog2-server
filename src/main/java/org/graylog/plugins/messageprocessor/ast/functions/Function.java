package org.graylog.plugins.messageprocessor.ast.functions;

import org.graylog.plugins.messageprocessor.ast.expressions.Expression;

import java.util.List;

public interface Function {

    Object evaluate(List<Expression> args);

    FunctionDescriptor descriptor();

}
