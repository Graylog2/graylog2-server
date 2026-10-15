/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.pipelineprocessor.ast.expressions;

import com.google.common.base.CaseFormat;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Ints;
import com.swrve.ratelimitedlogger.RateLimitedLog;
import org.apache.commons.beanutils.PropertyUtils;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.graylog2.plugin.utilities.ratelimitedlog.RateLimitedLogFactory.createDefaultRateLimitedLog;

/**
 * Operator semantics shared by the AST interpreter and compiled rules.
 * <p>
 * Bodies are moved verbatim from the expression nodes, including local variable and parameter names: helpful
 * NullPointerException messages name them, and both engines must produce identical error texts.
 */
public final class PipelineOps {
    // keep the log categories of the nodes the code came from
    private static final RateLimitedLog EQUALITY_LOG = createDefaultRateLimitedLog(EqualityExpression.class);
    private static final RateLimitedLog FIELD_ACCESS_LOG = createDefaultRateLimitedLog(FieldAccessExpression.class);

    private PipelineOps() {
    }

    /** From {@link ComparisonExpression#evaluateBool}. */
    public static boolean compare(String operator, Object leftValue, Object rightValue) {
        if (leftValue instanceof DateTime && rightValue instanceof DateTime) {
            return compareDateTimes(operator, (DateTime) leftValue, (DateTime) rightValue);
        }
        if (leftValue instanceof Double || rightValue instanceof Double) {
            return compareDouble(operator, (double) leftValue, (double) rightValue);
        }
        return compareLong(operator, (long) leftValue, (long) rightValue);
    }

    @SuppressWarnings("Duplicates")
    private static boolean compareLong(String operator, long left, long right) {
        switch (operator) {
            case ">":
                return left > right;
            case ">=":
                return left >= right;
            case "<":
                return left < right;
            case "<=":
                return left <= right;
            default:
                return false;
        }
    }

    @SuppressWarnings("Duplicates")
    private static boolean compareDouble(String operator, double left, double right) {
        switch (operator) {
            case ">":
                return left > right;
            case ">=":
                return left >= right;
            case "<":
                return left < right;
            case "<=":
                return left <= right;
            default:
                return false;
        }
    }

    private static boolean compareDateTimes(String operator, DateTime left, DateTime right) {
        switch (operator) {
            case ">":
                return left.isAfter(right);
            case ">=":
                return !left.isBefore(right);
            case "<":
                return left.isBefore(right);
            case "<=":
                return !left.isAfter(right);
            default:
                return false;
        }
    }

    /** From {@link EqualityExpression#evaluateBool}. */
    public static boolean equality(EqualityExpression node, EvaluationContext context, Object left, Object right) {
        if (left == null) {
            EQUALITY_LOG.warn(context.pipelineErrorMessage("left expression evaluated to null, returning false: " + node.left()));
            return false;
        }
        final boolean equals;
        // sigh: DateTime::equals takes the chronology into account, so identical instants expressed in different timezones are not equal
        if (left instanceof DateTime && right instanceof DateTime) {
            equals = ((DateTime) left).isEqual((DateTime) right);
        } else {
            equals = left.equals(right);
        }

        if (EQUALITY_LOG.isTraceEnabled()) {
            traceEquality(node, left, right, equals, node.isCheckEquality());
        }
        if (node.isCheckEquality()) {
            return equals;
        }
        return !equals;
    }

    private static void traceEquality(EqualityExpression node,
                                      Object left,
                                      Object right,
                                      boolean equals,
                                      boolean checkEquality) {
        EQUALITY_LOG.trace(checkEquality
                        ? "[{}] {} == {} : {} == {}"
                        : "[{}] {} != {} : {} != {}",
                checkEquality == equals, node.left(), node.right(), left, right);
    }

    /** From {@link AdditionExpression#evaluateUnsafe}. */
    @Nullable
    public static Object add(AdditionExpression node, Object leftValue, Object rightValue) {
        // special case for date arithmetic
        final boolean leftDate = DateTime.class.equals(leftValue.getClass());
        final boolean leftPeriod = Period.class.equals(leftValue.getClass());
        final boolean rightDate = DateTime.class.equals(rightValue.getClass());
        final boolean rightPeriod = Period.class.equals(rightValue.getClass());

        if (leftDate && rightPeriod) {
            final DateTime date = (DateTime) leftValue;
            final Period period = (Period) rightValue;

            return node.isPlus() ? date.plus(period) : date.minus(period);
        } else if (leftPeriod && rightDate) {
            final DateTime date = (DateTime) rightValue;
            final Period period = (Period) leftValue;

            return node.isPlus() ? date.plus(period) : date.minus(period);
        } else if (leftPeriod && rightPeriod) {
            final Period period1 = (Period) leftValue;
            final Period period2 = (Period) rightValue;

            return node.isPlus() ? period1.plus(period2) : period1.minus(period2);
        } else if (leftDate && rightDate) {
            // the most uncommon, this is only defined for - really and means "interval between them"
            // because adding two dates makes no sense
            if (node.isPlus()) {
                // makes no sense to compute and should be handles in the parser already
                return null;
            }
            final DateTime left = (DateTime) leftValue;
            final DateTime right = (DateTime) rightValue;

            if (left.isBefore(right)) {
                return new Duration(left, right);
            } else {
                return new Duration(right, left);
            }
        } else if (String.class.equals(node.getType())) {
            if (!node.isPlus()) {
                return null;
            }
            final String left = String.valueOf(leftValue);
            final String right = String.valueOf(rightValue);
            return left + right;
        }
        if (node.isIntegral()) {
            final long l = (long) leftValue;
            final long r = (long) rightValue;
            if (node.isPlus()) {
                return l + r;
            } else {
                return l - r;
            }
        } else {
            final double l = (double) leftValue;
            final double r = (double) rightValue;
            if (node.isPlus()) {
                return l + r;
            } else {
                return l - r;
            }
        }
    }

    /** From {@link MultiplicationExpression#evaluateUnsafe}. */
    @SuppressWarnings("Duplicates")
    @Nullable
    public static Object multiply(MultiplicationExpression node, EvaluationContext context, Object leftValue, Object rightValue) {
        if (node.isIntegral()) {
            long l = (long) leftValue;
            long r = (long) rightValue;
            switch (node.getOperator()) {
                case '*':
                    return l * r;
                case '/':
                    return l / r;
                case '%':
                    return l % r;
                default:
                    throw new IllegalStateException(
                            context.pipelineErrorMessage("Invalid operator, this is a bug."));
            }
        } else {
            final double l = (double) leftValue;
            final double r = (double) rightValue;

            switch (node.getOperator()) {
                case '*':
                    return l * r;
                case '/':
                    return l / r;
                case '%':
                    return l % r;
                default:
                    throw new IllegalStateException(
                            context.pipelineErrorMessage("Invalid operator, this is a bug."));
            }
        }
    }

    /** From {@link SignedExpression#evaluateUnsafe}. */
    @Nullable
    public static Object signed(SignedExpression node, EvaluationContext context, Object value) {
        if (value instanceof Long) {
            long number = (long) value;
            return node.isPlus() ? +number : -number;
        } else if (value instanceof Double) {
            double number = (double) value;
            return node.isPlus() ? +number : -number;
        }
        // nothing we could handle, the type checker should've caught it
        throw new IllegalArgumentException(
                context.pipelineErrorMessage("Value of '" + node.right().toString() + "' is not a number: " + value));
    }

    /** From {@link IndexedAccessExpression#evaluateUnsafe}. */
    public static Object indexedAccess(EvaluationContext context, Object idxObj, Object indexable) {
        if (idxObj == null || indexable == null) {
            return null;
        }

        if (idxObj instanceof Long) {
            int idx = Ints.saturatedCast((long) idxObj);
            if (indexable.getClass().isArray()) {
                return Array.get(indexable, idx);
            } else if (indexable instanceof List) {
                return ((List) indexable).get(idx);
            } else if (indexable instanceof Iterable) {
                return Iterables.get((Iterable) indexable, idx);
            }
            throw new IllegalArgumentException(
                    context.pipelineErrorMessage("Object '" + indexable + "' is not an Array, List or Iterable."));
        } else if (idxObj instanceof String) {
            final String idx = idxObj.toString();
            if (indexable instanceof Map) {
                return ((Map) indexable).get(idx);
            }
            throw new IllegalArgumentException(
                    context.pipelineErrorMessage("Object '" + indexable + "' is not a Map."));
        }
        throw new IllegalArgumentException(
                context.pipelineErrorMessage("Index '" + idxObj + "' is not a Long or String."));
    }

    /** From {@link FieldAccessExpression#evaluateUnsafe}. */
    public static Object fieldAccess(Object bean, Object fieldValue) {
        if (bean == null || fieldValue == null) {
            return null;
        }
        final String fieldName = fieldValue.toString();

        // First try to access the field using the given field name
        final Object property = getProperty(bean, fieldName);
        if (property == null) {
            // If the given field name does not work, try to convert it to camel case to make JSON-like access
            // to fields possible. Example: "geo.location.metro_code" => "geo.getLocation().getMetroCode()"
            return getProperty(bean, CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, fieldName));
        }
        return property;
    }

    private static Object getProperty(Object bean, String fieldName) {
        try {
            Object property = PropertyUtils.getProperty(bean, fieldName);
            if (property == null) {
                // in case the bean is a Map, try again with a simple property, it might be masked by the Map
                property = PropertyUtils.getSimpleProperty(bean, fieldName);
            }
            FIELD_ACCESS_LOG.debug("[field access] property {} of bean {}: {}", fieldName, bean.getClass().getTypeName(), property);
            return property;
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            FIELD_ACCESS_LOG.debug("Unable to read property {} from {}", fieldName, bean);
            return null;
        }
    }

    /** From {@link MessageRefExpression#evaluateUnsafe}, for a non-null field expression. */
    public static Object messageField(EvaluationContext context, Object fieldName) {
        if (fieldName == null) {
            return null;
        }
        return context.currentMessage().getField(fieldName.toString());
    }

    /** From {@link BooleanValuedFunctionWrapper#evaluateBool}. */
    public static boolean toBool(Object value) {
        return value != null && (Boolean) value;
    }

    /** From {@link ArrayLiteralExpression#evaluateUnsafe}: same collector, so the same list type. */
    public static List<Object> arrayLiteral(Object[] values) {
        return Arrays.stream(values).collect(Collectors.toList());
    }

    /** From {@link MapLiteralExpression#evaluateUnsafe}: same Seq pipeline, so the same map type. */
    public static Map<String, Object> mapLiteral(String[] keys, Object[] values) {
        return Seq.zip(Seq.of(keys), Seq.of(values))
                .filter(entry -> entry.v2 != null) // filter out null values, which would throw exceptions.
                .toMap(Tuple2::v1, Tuple2::v2);
    }
}
