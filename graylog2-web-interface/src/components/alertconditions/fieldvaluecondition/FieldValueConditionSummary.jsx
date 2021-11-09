/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import PropTypes from 'prop-types';
import React from 'react';

import GracePeriodSummary from 'components/alertconditions/GracePeriodSummary';
import BacklogSummary from 'components/alertconditions/BacklogSummary';
import RepeatNotificationsSummary from 'components/alertconditions/RepeatNotificationsSummary';
import { Pluralize } from 'components/common';

class FieldValueConditionSummary extends React.Component {
  static propTypes = {
    alertCondition: PropTypes.object.isRequired,
  };

  render() {
    const { alertCondition } = this.props;
    const { field } = alertCondition.parameters;
    const { threshold } = alertCondition.parameters;
    const thresholdType = alertCondition.parameters.threshold_type.toLocaleLowerCase('en');
    const type = alertCondition.parameters.type.toLocaleLowerCase('en');
    const { time } = alertCondition.parameters;

    return (
      <span>
        Alert is triggered when the field {field} has a {thresholdType}
        {' '}{type} value than {threshold} in the
        {' '}
        <Pluralize value={time} singular="last minute" plural={`last ${time} minutes`} />.
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

export default FieldValueConditionSummary;
