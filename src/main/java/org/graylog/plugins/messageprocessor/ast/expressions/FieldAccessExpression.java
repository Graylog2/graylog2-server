package org.graylog.plugins.messageprocessor.ast.expressions;

import org.apache.commons.lang3.StringUtils;
import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog2.plugin.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class FieldAccessExpression implements Expression {
    private static final Logger log = LoggerFactory.getLogger(FieldAccessExpression.class);

    private final Expression object;
    private final Expression field;

    public FieldAccessExpression(Expression object, Expression field) {
        this.object = object;
        this.field = field;
    }

    @Override
    public boolean isConstant() {
        return false;
    }

    @Override
    public Object evaluate(EvaluationContext context, Message message) {
        final Object bean = this.object.evaluate(context, message);
        final String fieldName = field.evaluate(context, message).toString();
        try {
            final Method method = bean.getClass().getMethod("get"+ StringUtils.capitalize(fieldName));
            return method.invoke(bean);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            log.error("Oops");
            return null;
        }
    }

    @Override
    public Class getType() {
        return Object.class;
    }

    @Override
    public String toString() {
        return object.toString() + "." + field.toString();
    }
}
