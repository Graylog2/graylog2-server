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
import numeral from 'numeral';

class GaugeDetails extends React.Component {
  static propTypes = {
    metric: PropTypes.object.isRequired,
  };

  render() {
    const gauge = this.props.metric.metric;

    return (
      <dl className="metric-def metric-gauge">
        <dt>Value:</dt>
        <dd><span className="number-format">{numeral(gauge.value).format('0,0')}</span></dd>
      </dl>
    );
  }
}

export default GaugeDetails;
