const AggregationExpressionParser = require('./AggregationExpressionParser');

describe('AggregationExpressionParser', () => {
  it('parses a simple expression', () => {
    const expression = {
      expression: {
        expr: '>',
        left: {
          expr: 'number-ref',
          ref: '123',
        },
        right: {
          expr: 'number',
          value: 42,
        },
      },
    };
    const parsedResults = AggregationExpressionParser.parseExpression(expression);
    expect(typeof parsedResults).toBe('object');

    const { seriesId, operator, value } = parsedResults;
    expect(seriesId).toBe('123');
    expect(operator).toBe('>');
    expect(value).toBe(42);
  });

  it('parses an empty expression', () => {
    const expression = {
      expression: null,
    };
    const parsedResults = AggregationExpressionParser.parseExpression(expression);
    expect(typeof parsedResults).toBe('object');
    expect(Object.keys(parsedResults).length).toBe(0);
  });

  it('parses an expression without expr', () => {
    const expression = {
      expression: {
        left: {
          expr: 'number-ref',
          ref: '123',
        },
        right: {
          expr: 'number',
          value: 42,
        },
      },
    };
    const parsedResults = AggregationExpressionParser.parseExpression(expression);
    expect(typeof parsedResults).toBe('object');

    const { seriesId, operator, value } = parsedResults;
    expect(seriesId).toBe('123');
    expect(operator).toBe(undefined);
    expect(value).toBe(42);
  });

  it('parses an expression without ref', () => {
    const expression = {
      expression: {
        expr: '>',
        left: {
          expr: 'number-ref',
        },
        right: {
          expr: 'number',
          value: 42,
        },
      },
    };
    const parsedResults = AggregationExpressionParser.parseExpression(expression);
    expect(typeof parsedResults).toBe('object');

    const { seriesId, operator, value } = parsedResults;
    expect(seriesId).toBe(undefined);
    expect(operator).toBe('>');
    expect(value).toBe(42);
  });

  it('parses an expression without value', () => {
    const expression = {
      expression: {
        expr: '>',
        left: {
          expr: 'number-ref',
          ref: '123',
        },
        right: {
          expr: 'number',
        },
      },
    };
    const parsedResults = AggregationExpressionParser.parseExpression(expression);
    expect(typeof parsedResults).toBe('object');

    const { seriesId, operator, value } = parsedResults;
    expect(seriesId).toBe('123');
    expect(operator).toBe('>');
    expect(value).toBe(undefined);
  });
});
