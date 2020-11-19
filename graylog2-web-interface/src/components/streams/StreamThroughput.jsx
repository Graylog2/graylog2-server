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

import StoreProvider from 'injection/StoreProvider';
import ActionsProvider from 'injection/ActionsProvider';
import { Spinner } from 'components/common';

const MetricsStore = StoreProvider.getStore('Metrics');
const MetricsActions = ActionsProvider.getActions('Metrics');

const StreamThroughput = createReactClass({
  displayName: 'StreamThroughput',

  propTypes: {
    streamId: PropTypes.string.isRequired,
  },

  mixins: [Reflux.connect(MetricsStore)],

  // eslint-disable-next-line camelcase
  UNSAFE_componentWillMount() {
    MetricsActions.addGlobal(this._metricName());
  },

  componentWillUnmount() {
    MetricsActions.removeGlobal(this._metricName());
  },

  _metricName() {
    return `org.graylog2.plugin.streams.Stream.${this.props.streamId}.incomingMessages.1-sec-rate`;
  },

  _calculateThroughput() {
    return Object.keys(this.state.metrics)
      .map((nodeId) => {
        const metricDefinition = this.state.metrics[nodeId][this._metricName()];

        return metricDefinition !== undefined ? metricDefinition.metric.value : 0;
      })
      .reduce((throughput1, throughput2) => throughput1 + throughput2, 0);
  },

  render() {
    if (!this.state.metrics) {
      return <Spinner />;
    }

    return (
      <span>{this._calculateThroughput()} messages/second</span>
    );
  },
});

export default StreamThroughput;
