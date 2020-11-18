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
export const emptyComparisonExpressionConfig = () => {
  return {
    expr: undefined,
    left: {
      expr: 'number-ref',
      ref: undefined,
    },
    right: {
      expr: 'number',
      value: 0,
    },
  };
};

export const emptyBooleanExpressionConfig = ({ operator = '&&', left = emptyComparisonExpressionConfig(), right = emptyComparisonExpressionConfig() }) => {
  return {
    expr: operator,
    left: left,
    right: right,
  };
};

export const emptyGroupExpressionConfig = ({ operator = '&&', child = emptyComparisonExpressionConfig() }) => {
  return {
    expr: 'group',
    operator: operator,
    child: child,
  };
};

/**
 * Replaces all boolean expressions inside the current tree, without affecting any internal groups.
 */
export const replaceBooleanExpressionOperatorInGroup = (nextOperator, expression) => {
  const nextExpression = { ...expression };

  if (expression.expr === '&&' || expression.expr === '||') {
    nextExpression.expr = nextOperator;
    nextExpression.left = replaceBooleanExpressionOperatorInGroup(nextOperator, nextExpression.left);
    nextExpression.right = replaceBooleanExpressionOperatorInGroup(nextOperator, nextExpression.right);
  }

  return nextExpression;
};
