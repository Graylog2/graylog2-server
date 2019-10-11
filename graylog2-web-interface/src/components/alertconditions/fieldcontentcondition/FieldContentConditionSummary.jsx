import PropTypes from 'prop-types';
import React from 'react';

import GracePeriodSummary from 'components/alertconditions/GracePeriodSummary';
import BacklogSummary from 'components/alertconditions/BacklogSummary';
import RepeatNotificationsSummary from 'components/alertconditions/RepeatNotificationsSummary';

class FieldContentConditionSummary extends React.Component {
  static propTypes = {
    alertCondition: PropTypes.object.isRequired,
  };

  _formatMatcher = (field, value) => {
    return <span>{`\<${field}: "${value}"\>`}</span>;
  };

  render() {
    const { alertCondition } = this.props;
    const { field } = alertCondition.parameters;
    const { value } = alertCondition.parameters;

    return (
      <span>
        Alert is triggered when messages matching {this._formatMatcher(field, value)} are received.
        {' '}
        <GracePeriodSummary alertCondition={alertCondition} />
        {' '}
        <BacklogSummary alertCondition={alertCondition} />
        {' '}
        <RepeatNotificationsSummary alertCondition={alertCondition} />
      </span>
    );
  }
}

export default FieldContentConditionSummary;
