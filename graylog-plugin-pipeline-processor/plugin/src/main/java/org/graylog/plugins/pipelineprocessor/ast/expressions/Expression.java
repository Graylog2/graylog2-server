/**
 * This file is part of Graylog Pipeline Processor.
 *
 * Graylog Pipeline Processor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog Pipeline Processor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog Pipeline Processor.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.pipelineprocessor.ast.expressions;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;

import org.antlr.v4.runtime.Token;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.exceptions.FunctionEvaluationException;

import java.util.Map;

import javax.annotation.Nullable;

import static org.graylog2.shared.utilities.ExceptionUtils.getRootCause;

public interface Expression {

    boolean isConstant();

    Token getStartToken();

    @Nullable
    default Object evaluate(EvaluationContext context) {
        try {
            return evaluateUnsafe(context);
        } catch (FunctionEvaluationException fee) {
            context.addEvaluationError(fee.getStartToken().getLine(),
                                       fee.getStartToken().getCharPositionInLine(),
                                       fee.getFunctionExpression().getFunction().descriptor(),
                                       getRootCause(fee));
        } catch (Exception e) {
            context.addEvaluationError(getStartToken().getLine(), getStartToken().getCharPositionInLine(), null, getRootCause(e));
        }
        return null;
    }

    Class getType();

    /**
     * This method is allowed to throw exceptions. The outside world is supposed to call evaluate instead.
     */
    @Nullable
    Object evaluateUnsafe(EvaluationContext context);

    /**
     * This method is allowed to throw exceptions and evaluates the expression in an empty context.
     * It is only useful for the interpreter/code generator to evaluate constant expressions to their effective value.
     *
     * @return the value of the expression in an empty context
     */
    default Object evaluateUnsafe() {
        return evaluateUnsafe(EvaluationContext.emptyContext());
    }

    Iterable<Expression> children();

    default Type nodeType() {
        return Type.fromClass(this.getClass());
    }

    // helper to aid switching over the available expression node types
    enum Type {
        ADD(AdditionExpression.class),
        AND(AndExpression.class),
        ARRAY_LITERAL(ArrayLiteralExpression.class),
        BINARY(BinaryExpression.class),
        BOOLEAN(BooleanExpression.class),
        BOOLEAN_FUNC_WRAPPER(BooleanValuedFunctionWrapper.class),
        COMPARISON(ComparisonExpression.class),
        CONSTANT(ConstantExpression.class),
        DOUBLE(DoubleExpression.class),
        EQUALITY(EqualityExpression.class),
        FIELD_ACCESS(FieldAccessExpression.class),
        FIELD_REF(FieldRefExpression.class),
        FUNCTION(FunctionExpression.class),
        INDEXED_ACCESS(IndexedAccessExpression.class),
        LOGICAL(LogicalExpression.class),
        LONG(LongExpression.class),
        MAP_LITERAL(MapLiteralExpression.class),
        MESSAGE(MessageRefExpression.class),
        MULT(MultiplicationExpression.class),
        NOT(NotExpression.class),
        NUMERIC(NumericExpression.class),
        OR(OrExpression.class),
        SIGNED(SignedExpression.class),
        STRING(StringExpression.class),
        UNARY(UnaryExpression.class),
        VAR_REF(VarRefExpression.class);

        static Map<Class, Type> classMap;

        static {
            classMap = Maps.uniqueIndex(Iterators.forArray(Type.values()), type -> type.klass);
        }

        private final Class<? extends Expression> klass;

        Type(Class<? extends Expression> expressionClass) {
            klass = expressionClass;
        }

        static Type fromClass(Class klass) {
            return classMap.get(klass);
        }
    }
}
