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
package org.graylog.plugins.pipelineprocessor.ast;

import org.graylog.plugins.pipelineprocessor.ast.expressions.AdditionExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.AndExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.ArrayLiteralExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.BinaryExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.BooleanExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.BooleanValuedFunctionWrapper;
import org.graylog.plugins.pipelineprocessor.ast.expressions.ComparisonExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.ConstantExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.DoubleExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.EqualityExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.Expression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.FieldAccessExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.FieldRefExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.FunctionExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.IndexedAccessExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.LogicalExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.LongExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.MapLiteralExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.MessageRefExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.MultiplicationExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.NotExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.NumericExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.OrExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.SignedExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.StringExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.UnaryExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.VarRefExpression;
import org.graylog.plugins.pipelineprocessor.ast.statements.FunctionStatement;
import org.graylog.plugins.pipelineprocessor.ast.statements.Statement;
import org.graylog.plugins.pipelineprocessor.ast.statements.VarAssignStatement;

import java.util.Collection;

public class RuleAstWalker {

    public void walk(RuleAstListener listener, Rule rule) {
        listener.enterRule(rule);

        listener.enterWhen(rule);

        walkExpression(listener, rule.when());

        listener.exitWhen(rule);

        listener.enterThen(rule);

        walkStatements(listener, rule.then());

        listener.exitThen(rule);

        listener.exitRule(rule);
    }

    private void walkExpression(RuleAstListener listener, Expression expr) {
        listener.enterEveryExpression(expr);
        triggerAbstractEnter(listener, expr);
        switch (expr.nodeType()) {
            case ADD:
                listener.enterAddition((AdditionExpression) expr);
                visitChildren(listener, expr);
                listener.exitAddition((AdditionExpression) expr);
                break;
            case AND:
                listener.enterAnd((AndExpression) expr);
                visitChildren(listener, expr);
                listener.exitAnd((AndExpression) expr);
                break;
            case ARRAY_LITERAL:
                listener.enterArrayLiteral((ArrayLiteralExpression) expr);
                visitChildren(listener, expr);
                listener.exitArrayLiteral((ArrayLiteralExpression) expr);
                break;
            case BINARY:
                // special, handled as wrapper type in triggerAbstractEnter/Exit
                break;
            case BOOLEAN:
                listener.enterBoolean((BooleanExpression) expr);
                visitChildren(listener, expr);
                listener.exitBoolean((BooleanExpression) expr);
                break;
            case BOOLEAN_FUNC_WRAPPER:
                listener.enterBooleanFuncWrapper((BooleanValuedFunctionWrapper) expr);
                visitChildren(listener, expr);
                listener.exitBooleanFuncWrapper((BooleanValuedFunctionWrapper) expr);
                break;
            case COMPARISON:
                listener.enterComparison((ComparisonExpression) expr);
                visitChildren(listener, expr);
                listener.exitComparison((ComparisonExpression) expr);
                break;
            case CONSTANT:
                // special, handled as wrapper type in triggerAbstractEnter/Exit
                break;
            case DOUBLE:
                listener.enterDouble((DoubleExpression) expr);
                visitChildren(listener, expr);
                listener.exitDouble((DoubleExpression) expr);
                break;
            case EQUALITY:
                listener.enterEquality((EqualityExpression) expr);
                visitChildren(listener, expr);
                listener.exitEquality((EqualityExpression) expr);
                break;
            case FIELD_ACCESS:
                listener.enterFieldAccess((FieldAccessExpression) expr);
                visitChildren(listener, expr);
                listener.exitFieldAccess((FieldAccessExpression) expr);
                break;
            case FIELD_REF:
                listener.enterFieldRef((FieldRefExpression) expr);
                visitChildren(listener, expr);
                listener.exitFieldRef((FieldRefExpression) expr);
                break;
            case FUNCTION:
                listener.enterFunctionCall((FunctionExpression) expr);
                // special case, we want to wrap each function argument's expressing into its own
                // callback, so we can generate statements for them.
                expr.children().forEach(expression -> {
                    listener.enterFunctionArg((FunctionExpression) expr, expression);
                    walkExpression(listener, expression);
                    listener.exitFunctionArg(expression);
                });

                listener.exitFunctionCall((FunctionExpression) expr);
                break;
            case INDEXED_ACCESS:
                listener.enterIndexedAccess((IndexedAccessExpression) expr);
                visitChildren(listener, expr);
                listener.exitIndexedAccess((IndexedAccessExpression) expr);
                break;
            case LOGICAL:
                // special, handled as wrapper type in triggerAbstractEnter/Exit
                break;
            case LONG:
                listener.enterLong((LongExpression) expr);
                visitChildren(listener, expr);
                listener.exitLong((LongExpression) expr);
                break;
            case MAP_LITERAL:
                listener.enterMapLiteral((MapLiteralExpression) expr);
                visitChildren(listener, expr);
                listener.exitMapLiteral((MapLiteralExpression) expr);
                break;
            case MESSAGE:
                listener.enterMessageRef((MessageRefExpression) expr);
                visitChildren(listener, expr);
                listener.exitMessageRef((MessageRefExpression) expr);
                break;
            case MULT:
                listener.enterMultiplication((MultiplicationExpression) expr);
                visitChildren(listener, expr);
                listener.exitMultiplication((MultiplicationExpression) expr);
                break;
            case NOT:
                listener.enterNot((NotExpression) expr);
                visitChildren(listener, expr);
                listener.exitNot((NotExpression) expr);
                break;
            case NUMERIC:
                // special, handled as wrapper type in triggerAbstractEnter/Exit
                break;
            case OR:
                listener.enterOr((OrExpression) expr);
                visitChildren(listener, expr);
                listener.exitOr((OrExpression) expr);
                break;
            case SIGNED:
                listener.enterSigned((SignedExpression) expr);
                visitChildren(listener, expr);
                listener.exitSigned((SignedExpression) expr);
                break;
            case STRING:
                listener.enterString((StringExpression) expr);
                visitChildren(listener, expr);
                listener.exitString((StringExpression) expr);
                break;
            case UNARY:
                // special, handled as wrapper type in triggerAbstractEnter/Exit
                break;
            case VAR_REF:
                listener.enterVariableReference((VarRefExpression) expr);
                visitChildren(listener, expr);
                listener.exitVariableReference((VarRefExpression) expr);
                break;
        }
        triggerAbstractExit(listener, expr);
        listener.exitEveryExpression(expr);
    }

    private void triggerAbstractEnter(RuleAstListener listener, Expression expr) {
        
        if (expr instanceof BinaryExpression) {
            listener.enterBinary((BinaryExpression) expr);
            
        } else if (expr instanceof UnaryExpression) { // must not be first in "else if" because "binary is instanceof unary" 
            listener.enterUnary((UnaryExpression) expr);
        }
        // for the others we trigger regardless whether it's a binary or unary expr
        if (expr instanceof LogicalExpression) {
            listener.enterLogical((LogicalExpression) expr);
        }
        if (expr instanceof NumericExpression) {
            listener.enterNumeric((NumericExpression) expr);
        }
        if (expr instanceof ConstantExpression) {
            listener.enterConstant((ConstantExpression) expr);
        }
    }

    private void triggerAbstractExit(RuleAstListener listener, Expression expr) {
        if (expr instanceof BinaryExpression) {
            listener.exitBinary((BinaryExpression) expr);

        } else if (expr instanceof UnaryExpression) { // must not be first in "else if" because "binary is instanceof unary" 
            listener.exitUnary((UnaryExpression) expr);
        }
        // for the others we trigger regardless whether it's a binary or unary expr
        if (expr instanceof LogicalExpression) {
            listener.exitLogical((LogicalExpression) expr);
        }
        if (expr instanceof NumericExpression) {
            listener.exitNumeric((NumericExpression) expr);
        }
        if (expr instanceof ConstantExpression) {
            listener.exitConstant((ConstantExpression) expr);
        }
    }

    private void visitChildren(RuleAstListener listener, Expression expr) {
        expr.children().forEach(expression -> walkExpression(listener, expression));
    }

    private void walkStatements(RuleAstListener listener, Collection<Statement> statements) {
        statements.forEach(statement -> {
            listener.enterStatement(statement);

            if (statement instanceof FunctionStatement) {
                FunctionStatement func = (FunctionStatement) statement;
                listener.enterFunctionCallStatement(func);
                walkExpression(listener, func.getFunctionExpression());
                listener.exitFunctionCallStatement(func);
            } else if (statement instanceof VarAssignStatement) {
                VarAssignStatement assign = (VarAssignStatement) statement;
                listener.enterVariableAssignStatement(assign);
                walkExpression(listener, assign.getValueExpression());
                listener.exitVariableAssignStatement(assign);
            }

            listener.exitStatement(statement);
        });
    }
}
