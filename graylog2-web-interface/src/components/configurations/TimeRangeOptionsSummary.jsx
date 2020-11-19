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

class TimeRangeOptionsSummary extends React.Component {
  static propTypes = {
    options: PropTypes.object.isRequired,
  };

  render() {
    let timerangeOptionsSummary = null;

    if (this.props.options) {
      timerangeOptionsSummary = Object.keys(this.props.options).map((key, idx) => {
        return (
          <span key={`timerange-options-summary-${idx}`}>
            <dt>{key}</dt>
            <dd>{this.props.options[key]}</dd>
          </span>
        );
      });
    }

    return (
      <dl className="deflist">
        {timerangeOptionsSummary}
      </dl>
    );
  }
}

export default TimeRangeOptionsSummary;
