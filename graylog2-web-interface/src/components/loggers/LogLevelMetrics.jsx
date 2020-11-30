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
import numeral from 'numeral';

import { Col } from 'components/graylog';
import { Spinner } from 'components/common';
import CombinedProvider from 'injection/CombinedProvider';

const { MetricsStore, MetricsActions } = CombinedProvider.get('Metrics');

const LogLevelMetrics = createReactClass({
  displayName: 'LogLevelMetrics',

  propTypes: {
    nodeId: PropTypes.string.isRequired,
    loglevel: PropTypes.string.isRequired,
  },

  mixins: [Reflux.connect(MetricsStore)],

  componentDidMount() {
    MetricsActions.add(this.props.nodeId, this._metricName());
  },

  componentWillUnmount() {
    MetricsActions.remove(this.props.nodeId, this._metricName());
  },

  _metricName() {
    return `org.apache.logging.log4j.core.Appender.${this.props.loglevel}`;
  },

  render() {
    const { loglevel, nodeId } = this.props;
    const { metrics } = this.state;
    let metricsDetails;

    if (!metrics || !metrics[nodeId] || !metrics[nodeId][this._metricName()]) {
      metricsDetails = <Spinner />;
    } else {
      const { metric } = metrics[nodeId][this._metricName()];

      metricsDetails = (
        <dl className="loglevel-metrics-list">
          <dt>Total written:</dt>
          <dd><span className="loglevel-metric-total">{metric.rate.total}</span></dd>
          <dt>Mean rate:</dt>
          <dd><span className="loglevel-metric-mean">{numeral(metric.rate.mean).format('0.00')}</span> / second</dd>
          <dt>1 min rate:</dt>
          <dd><span className="loglevel-metric-1min">{numeral(metric.rate.one_minute).format('0.00')}</span> / second</dd>
        </dl>
      );
    }

    return (
      <div className="loglevel-metrics-row">
        <Col md={4}>
          <h3 className="u-light">Level: {lodash.capitalize(loglevel)}</h3>
          {metricsDetails}
        </Col>
      </div>
    );
  },
});

export default LogLevelMetrics;
