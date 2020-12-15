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
import numeral from 'numeral';

import { Spinner } from 'components/common';
import MetricsExtractor from 'logic/metrics/MetricsExtractor';
import StoreProvider from 'injection/StoreProvider';
import ActionsProvider from 'injection/ActionsProvider';

const MetricsStore = StoreProvider.getStore('Metrics');
const MetricsActions = ActionsProvider.getActions('Metrics');

// TODO this is a copy of GlobalTroughput, it just renders differently and only targets a single node.
const NodeThroughput = createReactClass({
  displayName: 'NodeThroughput',

  propTypes: {
    nodeId: PropTypes.string.isRequired,
    longFormat: PropTypes.bool,
  },

  mixins: [Reflux.connect(MetricsStore)],

  getDefaultProps() {
    return {
      longFormat: false,
    };
  },

  UNSAFE_componentWillMount() {
    this.metricNames = {
      totalIn: 'org.graylog2.throughput.input.1-sec-rate',
      totalOut: 'org.graylog2.throughput.output.1-sec-rate',
    };

    Object.keys(this.metricNames).forEach((metricShortName) => MetricsActions.add(this.props.nodeId, this.metricNames[metricShortName]));
  },

  componentWillUnmount() {
    Object.keys(this.metricNames).forEach((metricShortName) => MetricsActions.remove(this.props.nodeId, this.metricNames[metricShortName]));
  },

  _isLoading() {
    return !this.state.metrics;
  },

  _formatThroughput(metrics) {
    if (this.props.longFormat) {
      return (
        <span>
          Processing <strong>{numeral(metrics.totalIn).format('0,0')}</strong> incoming and <strong>
            {numeral(metrics.totalOut).format('0,0')}
                                                                                            </strong> outgoing msg/s.
        </span>
      );
    }

    return (
      <span>
        In {numeral(metrics.totalIn).format('0,0')} / Out {numeral(metrics.totalOut).format('0,0')} msg/s.
      </span>
    );
  },

  render() {
    if (this._isLoading()) {
      return <Spinner text="Loading throughput..." />;
    }

    const { nodeId } = this.props;
    const nodeMetrics = this.state.metrics[nodeId];
    const metrics = MetricsExtractor.getValuesForNode(nodeMetrics, this.metricNames);

    if (Object.keys(metrics).length === 0) {
      return (<span>Unable to load throughput.</span>);
    }

    return this._formatThroughput(metrics);
  },
});

export default NodeThroughput;
