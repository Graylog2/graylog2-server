import React from 'react';
import PropTypes from 'prop-types';

import { Row } from 'components/graylog';

import AggregationConditionExpression from './AggregationConditionExpression';

import commonStyles from '../common/commonStyles.css';

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
      onChange(key, { expression: update });
      return;
    }

    onChange(key, update);
  };

  render() {
    const { eventDefinition } = this.props;
    const { expression } = eventDefinition.config.conditions;

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
