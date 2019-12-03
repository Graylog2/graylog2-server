import uuid from 'uuid/v4';

export const emptyComparisonExpressionConfig = () => {
  return {
    expr: undefined,
    left: {
      expr: 'number-ref',
      ref: uuid(),
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
