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

class GracePeriodSummary extends React.Component {
  static propTypes = {
    alertCondition: PropTypes.object.isRequired,
  };

  _formatTime = (time) => {
    if (time === 1) {
      return '1 minute';
    }

    return `${time} minutes`;
  };

  render() {
    const time = this.props.alertCondition.parameters.grace;

    return <span>Grace period: {this._formatTime(time)}.</span>;
  }
}

export default GracePeriodSummary;
