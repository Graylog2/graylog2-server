import React from 'react';
import PropTypes from 'prop-types';

import { Row } from 'components/graylog';
import { emptyComparisonExpressionConfig } from 'logic/alerts/AggregationExpressionConfig';

import AggregationConditionExpression from './AggregationConditionExpression';

import commonStyles from '../common/commonStyles.css';

const initialEmptyConditionConfig = emptyComparisonExpressionConfig();

class AggregationConditionsForm extends React.Component {
  static propTypes = {
    eventDefinition: PropTypes.object.isRequired,
    validation: PropTypes.object.isRequired,
    formattedFields: PropTypes.array.isRequired,
    aggregationFunctions: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  handleChange = (key, update) => {
    const { onChange } = this.props;
    if (key === 'conditions') {
      // Propagate empty comparison expression, if the last expression was removed
      const nextUpdate = update || emptyComparisonExpressionConfig();
      onChange(key, { expression: nextUpdate });
      return;
    }

    onChange(key, update);
  };

  render() {
    const { eventDefinition } = this.props;
    const expression = eventDefinition.config.conditions.expression || initialEmptyConditionConfig;

    return (
      <React.Fragment>
        <h3 className={commonStyles.title}>Create Events for Definition</h3>

        <Row>
          <AggregationConditionExpression expression={expression} {...this.props} onChange={this.handleChange} />
        </Row>
      </React.Fragment>
    );
  }
}

export default AggregationConditionsForm;
