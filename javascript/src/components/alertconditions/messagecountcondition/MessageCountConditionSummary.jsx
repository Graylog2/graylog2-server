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
  _formatMessageThreshold(threshold, threshold_type) {
    if (threshold === 1) {
      return 'is ' + threshold_type + ' than one message';
    }

    return 'are ' + threshold_type + ' than ' + threshold + ' messages';
  },
  render() {
    const alertCondition = this.props.alertCondition;
    const threshold = alertCondition.parameters.threshold;
    const threshold_type = alertCondition.parameters.threshold_type;
    const time = alertCondition.parameters.time;

    return (
      <span>
        Alert is triggered when there {this._formatMessageThreshold(threshold, threshold_type)} in the {this._formatTime(time)}.
        {' '}
        <GracePeriodSummary alertCondition={alertCondition} />
        {' '}
        <BacklogSummary alertCondition={alertCondition}/>
      </span>
    );
  },
});

export default MessageCountConditionSummary;
