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

export const emptyBooleanExpressionConfig = ({ operator = '&&', expression }) => {
  return {
    expr: operator,
    left: expression,
    right: emptyComparisonExpressionConfig(),
  };
};
