import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import TimeHelper from 'util/TimeHelper';

import StoreProvider from 'injection/StoreProvider';

import ActionsProvider from 'injection/ActionsProvider';

import MetricsExtractor from 'logic/metrics/MetricsExtractor';

const MetricsStore = StoreProvider.getStore('Metrics');
const MetricsActions = ActionsProvider.getActions('Metrics');

const MetricContainer = createReactClass({
  displayName: 'MetricContainer',

  propTypes: {
    name: PropTypes.string.isRequired,
    zeroOnMissing: PropTypes.bool,
    children: PropTypes.node.isRequired,
  },

  mixins: [Reflux.connect(MetricsStore)],

  getDefaultProps() {
    return {
      zeroOnMissing: true,
    };
  },

  componentWillMount() {
    MetricsActions.addGlobal(this.props.name);
  },

  shouldComponentUpdate(_, nextState) {
    // Do not render this component and it's children when no metric data has changed.
    // This component and the CounterRate component expect to be rendered every second or less often. When using
    // these components on a page that triggers a re-render more often - e.g. by having another setInterval - the
    // calculation in CounterRate will break.
    if (this.state.metricsUpdatedAt && nextState.metricsUpdatedAt) {
      return nextState.metricsUpdatedAt > this.state.metricsUpdatedAt;
    }
    return true;
  },

  componentWillUnmount() {
    MetricsActions.removeGlobal(this.props.name);
  },

  render() {
    if (!this.state.metrics) {
      return (<span>Loading...</span>);
    }
    const fullName = this.props.name;
    let throughput = Object.keys(this.state.metrics)
      .map(nodeId => MetricsExtractor.getValuesForNode(this.state.metrics[nodeId], { throughput: fullName }))
      .reduce((one, two) => {
        return { throughput: (one.throughput || 0) + (two.throughput || 0) };
      }, {});
    if (this.props.zeroOnMissing && (!throughput || !throughput.throughput)) {
      throughput = { throughput: 0 };
    }
    return (
      <div>
        {
        React.Children.map(this.props.children, (child) => {
          return React.cloneElement(child, { metric: { full_name: fullName, count: throughput.throughput } });
        })
      }
      </div>
    );
  },
});

export default MetricContainer;
