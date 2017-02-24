import React from 'react';

import GracePeriodSummary from 'components/alertconditions/GracePeriodSummary';
import BacklogSummary from 'components/alertconditions/BacklogSummary';
import RepeatNotificationsSummary from 'components/alertconditions/RepeatNotificationsSummary';
import { Pluralize } from 'components/common';

const MessageCountConditionSummary = React.createClass({
  propTypes: {
    alertCondition: React.PropTypes.object.isRequired,
  },
  render() {
    const alertCondition = this.props.alertCondition;
    const threshold = alertCondition.parameters.threshold;
    const thresholdType = alertCondition.parameters.threshold_type.toLowerCase();
    const time = alertCondition.parameters.time;

    return (
      <span>
        Alert is triggered when there
        {' '}
        <Pluralize value={threshold} singular={`is ${thresholdType} than one message`}
                   plural={`are ${thresholdType} than ${threshold} messages`} />
        {' '}in the{' '}
        <Pluralize value={time} singular="last minute" plural={`last ${time} minutes`} />.
        {' '}
        <GracePeriodSummary alertCondition={alertCondition} />
        {' '}
        <BacklogSummary alertCondition={alertCondition} />
        {' '}
        <RepeatNotificationsSummary alertCondition={alertCondition} />
      </span>
    );
  },
});

export default MessageCountConditionSummary;
