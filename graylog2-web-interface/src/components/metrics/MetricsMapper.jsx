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

  componentWillMount() {
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
