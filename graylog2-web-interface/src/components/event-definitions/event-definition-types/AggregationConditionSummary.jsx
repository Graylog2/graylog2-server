import React from 'react';
import PropTypes from 'prop-types';

const AggregationConditionSummary = ({ conditions, series }) => {
  const renderExpression = (expression) => {
    switch (expression.expr) {
      case 'number':
        return expression.value;
      case 'number-ref':
        // eslint-disable-next-line no-case-declarations
        const selectedSeries = series.find(s => s.id === expression.ref);
        return (selectedSeries && selectedSeries.function
          ? <var>{selectedSeries.function}({selectedSeries.field})</var>
          : <span>No series selected</span>);
      case '&&':
      case '||':
        return (
          <>
            {renderExpression(expression.left)}{' '}
            <strong className="text-info">{expression.expr === '&&' ? 'AND' : 'OR'}</strong>{' '}
            {renderExpression(expression.right)}
          </>
        );
      case 'group':
        return <span>[{renderExpression(expression.child)}]</span>;
      case '<':
      case '<=':
      case '>':
      case '>=':
      case '==':
        return (
          <>
            {renderExpression(expression.left)}{' '}
            <strong className="text-primary">{expression.expr}{' '}</strong>
            {renderExpression(expression.right)}
          </>
        );
      default:
        return 'No condition configured';
    }
  };

  return renderExpression(conditions.expression);
};

AggregationConditionSummary.propTypes = {
  conditions: PropTypes.object.isRequired,
  series: PropTypes.array.isRequired,
};

export default AggregationConditionSummary;
