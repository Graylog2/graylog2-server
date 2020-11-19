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
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import lodash from 'lodash';

import { CounterDetails, GaugeDetails, HistogramDetails, MeterDetails, TimerDetails } from 'components/metrics';
import CombinedProvider from 'injection/CombinedProvider';

const { MetricsStore, MetricsActions } = CombinedProvider.get('Metrics');

const MetricDetails = createReactClass({
  displayName: 'MetricDetails',

  propTypes: {
    metric: PropTypes.object.isRequired,
  },

  mixins: [Reflux.connect(MetricsStore)],

  componentDidMount() {
    MetricsActions.add(this.props.nodeId, this.props.metric.full_name);
  },

  componentWillUnmount() {
    MetricsActions.remove(this.props.nodeId, this.props.metric.full_name);
  },

  _formatDetailsForType(type, metric) {
    switch (type) {
      case 'Counter':
        return <CounterDetails metric={metric} />;
      case 'Gauge':
        return <GaugeDetails metric={metric} />;
      case 'Histogram':
        return <HistogramDetails metric={metric} />;
      case 'Meter':
        return <MeterDetails metric={metric} />;
      case 'Timer':
        return <TimerDetails metric={metric} />;
      default:
        return <i>Invalid metric type: {type}</i>;
    }
  },

  render() {
    const metricName = this.props.metric.full_name;
    const { nodeId } = this.props;
    const metric = this.state.metrics && this.state.metrics[nodeId] && this.state.metrics[nodeId][metricName]
      ? this.state.metrics[nodeId][metricName] : this.props.metric;
    const type = lodash.capitalize(metric.type);
    const details = this._formatDetailsForType(type, metric);

    return (
      <div className="metric">
        <h3>{type}</h3>

        {details}
      </div>
    );
  },
});

export default MetricDetails;
