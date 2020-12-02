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

import CombinedProvider from 'injection/CombinedProvider';

const { MetricsActions, MetricsStore } = CombinedProvider.get('Metrics');

const MetricsMapper = createReactClass({
  displayName: 'MetricsMapper',

  propTypes: {
    map: PropTypes.object.isRequired,
    computeValue: PropTypes.func.isRequired,
  },

  mixins: [Reflux.connect(MetricsStore)],

  getDefaultProps() {
    return {
    };
  },

  getInitialState() {
    return {};
  },

  // eslint-disable-next-line camelcase
  UNSAFE_componentWillMount() {
    Object.keys(this.props.map).forEach((name) => MetricsActions.addGlobal(this.props.map[name]));
  },

  shouldComponentUpdate(_, nextState) {
    // Only re-render this component if the metric data has changed
    if (this.state.metricsUpdatedAt && nextState.metricsUpdatedAt) {
      return nextState.metricsUpdatedAt > this.state.metricsUpdatedAt;
    }

    return true;
  },

  componentWillUnmount() {
    Object.keys(this.props.map).forEach((name) => MetricsActions.removeGlobal(this.props.map[name]));
  },

  render() {
    if (!this.state.metrics) {
      return null;
    }

    const metricsMap = {};

    Object.keys(this.state.metrics).forEach((nodeId) => {
      Object.keys(this.props.map).forEach((key) => {
        const metricName = this.props.map[key];

        if (this.state.metrics[nodeId][metricName]) {
          // Only create the node entry if we actually have data
          if (!metricsMap[nodeId]) {
            metricsMap[nodeId] = {};
          }

          metricsMap[nodeId][key] = this.state.metrics[nodeId][metricName];
        }
      });
    });

    const value = this.props.computeValue(metricsMap);

    return (
      <span>
        {value}
      </span>
    );
  },
});

export default MetricsMapper;
