import lodash from 'lodash';

const AggregationExpressionParser = {
  generateExpression(seriesId, operator, value) {
    return {
      expression: {
        expr: operator,
        left: {
          expr: 'number-ref',
          ref: seriesId,
        },
        right: {
          expr: 'number',
          value: value,
        },
      },
    };
  },

  parseExpression(expression = {}) {
    if (expression === null || typeof expression !== 'object' || !expression.expression) {
      return {};
    }

    const operator = lodash.get(expression, 'expression.expr');
    let seriesId;
    let value;
    if (lodash.get(expression, 'expression.left.expr') === 'number-ref') {
      seriesId = lodash.get(expression, 'expression.left.ref');
    }
    if (lodash.get(expression, 'expression.right.expr') === 'number') {
      value = lodash.get(expression, 'expression.right.value');
    }

    return {
      seriesId: seriesId,
      operator: operator,
      value: value,
    };
  },
};

export default AggregationExpressionParser;
