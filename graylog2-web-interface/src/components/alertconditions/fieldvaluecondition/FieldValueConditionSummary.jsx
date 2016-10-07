import React from 'react';

import GracePeriodSummary from 'components/alertconditions/GracePeriodSummary';
import BacklogSummary from 'components/alertconditions/BacklogSummary';

const FieldValueConditionSummary = React.createClass({
  propTypes: {
    alertCondition: React.PropTypes.object.isRequired,
  },
  _formatTime(time) {
    if (time === 1) {
      return 'last minute';
    }

    return 'last ' + time + ' minutes';
  },
  render() {
    const alertCondition = this.props.alertCondition;
    const field = alertCondition.parameters.field;
    const threshold = alertCondition.parameters.threshold;
    const thresholdType = alertCondition.parameters.threshold_type.toLowerCase();
    const type = alertCondition.parameters.type;
    const time = alertCondition.parameters.time;

    return (
      <span>
        Alert is triggered when the field {field} has a {thresholdType}
        {' '}{type} value than {threshold} in the {this._formatTime(time)}.
        {' '}
        <GracePeriodSummary alertCondition={alertCondition} />
        {' '}
        <BacklogSummary alertCondition={alertCondition}/>
      </span>
    );
  },
});

export default FieldValueConditionSummary;
