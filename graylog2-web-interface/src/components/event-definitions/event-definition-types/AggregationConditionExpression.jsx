import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';

import NumberExpression from './AggregationConditionExpressions/NumberExpression';
import NumberRefExpression from './AggregationConditionExpressions/NumberRefExpression';
/* eslint-disable import/no-cycle */
// We render the expression tree recursively, so complex expressions need to refer back to this component
import BooleanExpression from './AggregationConditionExpressions/BooleanExpression';
import ComparisonExpression from './AggregationConditionExpressions/ComparisonExpression';
/* eslint-enable import/no-cycle */

class AggregationConditionExpression extends React.Component {
  static propTypes = {
    eventDefinition: PropTypes.object.isRequired,
    validation: PropTypes.object.isRequired,
    formattedFields: PropTypes.array.isRequired,
    aggregationFunctions: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired,
    expression: PropTypes.shape({
      expr: PropTypes.string,
      left: PropTypes.object,
      right: PropTypes.object,
    }).isRequired,
  };

  handleChildChange = (branch) => {
    return (key, update) => {
      const { expression, onChange } = this.props;

      let nextUpdate = update;
      if (key === 'conditions') {
        const nextExpression = lodash.cloneDeep(expression);
        nextExpression[branch] = update;
        nextUpdate = nextExpression;
      }

      onChange(key, nextUpdate);
    };
  };

  render() {
    const { expression, onChange, validation } = this.props;

    switch (expression.expr) {
      case 'number-ref':
        return <NumberRefExpression {...this.props} />;
      case 'number':
        return <NumberExpression expression={expression} onChange={onChange} validation={validation} />;
      case '&&':
      case '||':
        return <BooleanExpression {...this.props} onChildChange={this.handleChildChange} />;
      case '<':
      case '<=':
      case '>':
      case '>=':
      case '==':
      default:
        return <ComparisonExpression {...this.props} onChildChange={this.handleChildChange} />;
    }
  }
}

export default AggregationConditionExpression;
