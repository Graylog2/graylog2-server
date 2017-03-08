import React from 'react';

import { ConfigurationWell } from 'components/configurationforms';
import GracePeriodSummary from 'components/alertconditions/GracePeriodSummary';
import BacklogSummary from 'components/alertconditions/BacklogSummary';

const GenericAlertConditionSummary = React.createClass({
  propTypes: {
    alertCondition: React.PropTypes.object.isRequired,
  },

  render() {
    const alertCondition = this.props.alertCondition;
    const graceSummary = alertCondition.parameters.grace ? <GracePeriodSummary alertCondition={alertCondition} /> : null;
    const backlogSummary = alertCondition.parameters.backlog ? <BacklogSummary alertCondition={alertCondition} /> : null;
    return (
      <span>
        {graceSummary}
        {' '}
        {backlogSummary}
        <ConfigurationWell configuration={alertCondition.parameters} />
      </span>
    );
  },
});

export default GenericAlertConditionSummary;
