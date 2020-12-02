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

class HistogramDetails extends React.Component {
  static propTypes = {
    metric: PropTypes.object.isRequired,
  };

  render() {
    const histogram = this.props.metric.metric;

    return (
      <dl className="metric-def metric-histogram">
        <dt>95th percentile:</dt>
        <dd><span className="number-format">{numeral(histogram.time['95th_percentile']).format('0,0.[00]')}</span></dd>
        <dt>98th percentile:</dt>
        <dd><span className="number-format">{numeral(histogram.time['98th_percentile']).format('0,0.[00]')}</span></dd>
        <dt>99th percentile:</dt>
        <dd><span className="number-format">{numeral(histogram.time['99th_percentile']).format('0,0.[00]')}</span></dd>
        <dt>Standard deviation:</dt>
        <dd><span className="number-format">{numeral(histogram.time.std_dev).format('0,0.[00]')}</span></dd>
        <dt>Mean:</dt>
        <dd><span className="number-format">{numeral(histogram.time.mean).format('0,0.[00]')}</span></dd>
        <dt>Minimum:</dt>
        <dd><span className="number-format">{numeral(histogram.time.min).format('0,0.[00]')}</span></dd>
        <dt>Maximum:</dt>
        <dd><span className="number-format">{numeral(histogram.time.max).format('0,0.[00]')}</span></dd>
        <dt>Count:</dt>
        <dd><span className="number-format">{numeral(histogram.count).format('0,0')}</span></dd>
      </dl>
    );
  }
}

export default HistogramDetails;
