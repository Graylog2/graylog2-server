import React from 'react';

import GracePeriodSummary from 'components/alertconditions/GracePeriodSummary';
import BacklogSummary from 'components/alertconditions/BacklogSummary';

const MessageCountConditionSummary = React.createClass({
  propTypes: {
    alertCondition: React.PropTypes.object.isRequired,
  },
  _formatTime(time) {
    if (time === 1) {
      return 'last minute';
    }

    return 'last ' + time + ' minutes';
  },
  _formatMessageThreshold(threshold, thresholdType) {
    if (threshold === 1) {
      return 'is ' + thresholdType + ' than one message';
    }

    return 'are ' + thresholdType + ' than ' + threshold + ' messages';
  },
  render() {
    const alertCondition = this.props.alertCondition;
    const threshold = alertCondition.parameters.threshold;
    const thresholdType = alertCondition.parameters.threshold_type;
    const time = alertCondition.parameters.time;

    return (
      <span>
        Alert is triggered when there {this._formatMessageThreshold(threshold, thresholdType)} in the {this._formatTime(time)}.
        {' '}
        <GracePeriodSummary alertCondition={alertCondition} />
        {' '}
        <BacklogSummary alertCondition={alertCondition}/>
      </span>
    );
  },
});

export default MessageCountConditionSummary;
