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

import { ConfigurationWell } from 'components/configurationforms';
import GracePeriodSummary from 'components/alertconditions/GracePeriodSummary';
import BacklogSummary from 'components/alertconditions/BacklogSummary';

class GenericAlertConditionSummary extends React.Component {
  static propTypes = {
    alertCondition: PropTypes.object.isRequired,
  };

  render() {
    const { alertCondition } = this.props;
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
  }
}

export default GenericAlertConditionSummary;
