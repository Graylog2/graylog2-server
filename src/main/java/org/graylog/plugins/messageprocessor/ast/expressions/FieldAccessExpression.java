package org.graylog.plugins.messageprocessor.ast.expressions;

import org.apache.commons.beanutils.PropertyUtils;
import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

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
    public Object evaluate(EvaluationContext context) {
        final Object bean = this.object.evaluate(context);
        final String fieldName = field.evaluate(context).toString();
        try {
            final Object property = PropertyUtils.getProperty(bean, fieldName);
            log.debug("[field access] property {} of bean {}: {}", fieldName, bean.getClass().getTypeName(), property);
            return property;
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            log.error("Unable to read property {} from {}", fieldName, bean);
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
