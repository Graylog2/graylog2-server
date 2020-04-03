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
