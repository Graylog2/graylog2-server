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

/**
 * Consider using RuleAstBaseListener to only implement the callbacks relevant to you.
 */
public interface RuleAstListener {
    void enterRule(Rule rule);

    void exitRule(Rule rule);

    void enterWhen(Rule rule);

    void exitWhen(Rule rule);

    void enterThen(Rule rule);

    void exitThen(Rule rule);

    void enterStatement(Statement statement);

    void exitStatement(Statement statement);

    void enterFunctionCallStatement(FunctionStatement func);

    void exitFunctionCallStatement(FunctionStatement func);

    void enterVariableAssignStatement(VarAssignStatement assign);

    void exitVariableAssignStatement(VarAssignStatement assign);

    void enterAddition(AdditionExpression expr);

    void exitAddition(AdditionExpression expr);

    void enterAnd(AndExpression expr);

    void exitAnd(AndExpression expr);

    void enterArrayLiteral(ArrayLiteralExpression expr);

    void exitArrayLiteral(ArrayLiteralExpression expr);

    void enterBinary(BinaryExpression expr);

    void exitBinary(BinaryExpression expr);

    void enterBoolean(BooleanExpression expr);

    void exitBoolean(BooleanExpression expr);

    void enterBooleanFuncWrapper(BooleanValuedFunctionWrapper expr);

    void exitBooleanFuncWrapper(BooleanValuedFunctionWrapper expr);

    void enterComparison(ComparisonExpression expr);

    void exitComparison(ComparisonExpression expr);

    void enterConstant(ConstantExpression expr);

    void exitConstant(ConstantExpression expr);

    void enterDouble(DoubleExpression expr);

    void exitDouble(DoubleExpression expr);

    void enterEquality(EqualityExpression expr);

    void exitEquality(EqualityExpression expr);

    void enterFieldAccess(FieldAccessExpression expr);

    void exitFieldAccess(FieldAccessExpression expr);

    void enterFieldRef(FieldRefExpression expr);

    void exitFieldRef(FieldRefExpression expr);

    void enterFunctionCall(FunctionExpression expr);

    void exitFunctionCall(FunctionExpression expr);

    void enterIndexedAccess(IndexedAccessExpression expr);

    void exitIndexedAccess(IndexedAccessExpression expr);

    void enterLogical(LogicalExpression expr);

    void exitLogical(LogicalExpression expr);

    void enterLong(LongExpression expr);

    void exitLong(LongExpression expr);

    void enterMapLiteral(MapLiteralExpression expr);

    void exitMapLiteral(MapLiteralExpression expr);

    void enterMessageRef(MessageRefExpression expr);

    void exitMessageRef(MessageRefExpression expr);

    void enterMultiplication(MultiplicationExpression expr);

    void exitMultiplication(MultiplicationExpression expr);

    void enterNot(NotExpression expr);

    void exitNot(NotExpression expr);

    void enterNumeric(NumericExpression expr);

    void exitNumeric(NumericExpression expr);

    void enterOr(OrExpression expr);

    void exitOr(OrExpression expr);

    void enterSigned(SignedExpression expr);

    void exitSigned(SignedExpression expr);

    void enterString(StringExpression expr);

    void exitString(StringExpression expr);

    void enterUnary(UnaryExpression expr);

    void exitUnary(UnaryExpression expr);

    void enterVariableReference(VarRefExpression expr);

    void exitVariableReference(VarRefExpression expr);

    void enterEveryExpression(Expression expr);

    void exitEveryExpression(Expression expr);

    void enterFunctionArg(FunctionExpression functionExpression, Expression expression);

    void exitFunctionArg(Expression expression);
}
