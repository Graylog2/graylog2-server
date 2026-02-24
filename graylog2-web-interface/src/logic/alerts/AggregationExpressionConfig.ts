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
type NumberRefExpression = {
  expr: 'number-ref';
  ref: string | undefined;
};

type NumberExpression = {
  expr: 'number';
  value: number;
};

type ComparisonExpressionConfig = {
  expr: string | undefined;
  left: NumberRefExpression;
  right: NumberExpression;
};

type BooleanExpressionConfig = {
  expr: string;
  left: ExpressionConfig;
  right: ExpressionConfig;
};

type GroupExpressionConfig = {
  expr: 'group';
  operator: string;
  child: ExpressionConfig;
};

type ExpressionConfig = ComparisonExpressionConfig | BooleanExpressionConfig | GroupExpressionConfig;

export const emptyComparisonExpressionConfig = (): ComparisonExpressionConfig => ({
  expr: undefined,
  left: {
    expr: 'number-ref',
    ref: undefined,
  },
  right: {
    expr: 'number',
    value: 0,
  },
});

export const emptyBooleanExpressionConfig = ({
  operator = '&&',
  left = emptyComparisonExpressionConfig(),
  right = emptyComparisonExpressionConfig(),
}: {
  operator?: string;
  left?: ExpressionConfig;
  right?: ExpressionConfig;
}): BooleanExpressionConfig => ({
  expr: operator,
  left: left,
  right: right,
});

export const emptyGroupExpressionConfig = ({
  operator = '&&',
  child = emptyComparisonExpressionConfig(),
}: {
  operator?: string;
  child?: ExpressionConfig;
}): GroupExpressionConfig => ({
  expr: 'group',
  operator: operator,
  child: child,
});

/**
 * Replaces all boolean expressions inside the current tree, without affecting any internal groups.
 */
export const replaceBooleanExpressionOperatorInGroup = (
  nextOperator: string,
  expression: ExpressionConfig,
): ExpressionConfig => {
  if (expression.expr === '&&' || expression.expr === '||') {
    const boolExpr = expression as BooleanExpressionConfig;

    return {
      expr: nextOperator,
      left: replaceBooleanExpressionOperatorInGroup(nextOperator, boolExpr.left),
      right: replaceBooleanExpressionOperatorInGroup(nextOperator, boolExpr.right),
    };
  }

  return expression;
};
