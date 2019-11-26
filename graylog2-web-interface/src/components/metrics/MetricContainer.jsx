import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

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
    const { name } = this.props;
    MetricsActions.addGlobal(name);
  },

  shouldComponentUpdate(_, nextState) {
    // Do not render this component and it's children when no metric data has changed.
    // This component and the CounterRate component expect to be rendered every second or less often. When using
    // these components on a page that triggers a re-render more often - e.g. by having another setInterval - the
    // calculation in CounterRate will break.
    const { metricsUpdatedAt } = this.state;
    if (metricsUpdatedAt && nextState.metricsUpdatedAt) {
      return nextState.metricsUpdatedAt > metricsUpdatedAt;
    }
    return true;
  },

  componentWillUnmount() {
    const { name } = this.props;
    MetricsActions.removeGlobal(name);
  },

  render() {
    const { metrics } = this.state;
    if (!metrics) {
      return (<span>Loading...</span>);
    }
    const { children, name: fullName, zeroOnMissing } = this.props;
    let throughput = Object.keys(metrics)
      .map(nodeId => MetricsExtractor.getValuesForNode(metrics[nodeId], { throughput: fullName }))
      .reduce((one, two) => {
        return { throughput: (one.throughput || 0) + (two.throughput || 0) };
      }, {});
    if (zeroOnMissing && (!throughput || !throughput.throughput)) {
      throughput = { throughput: 0 };
    }
    return (
      <div>
        {
        React.Children.map(children, (child) => {
          return React.cloneElement(child, { metric: { full_name: fullName, count: throughput.throughput } });
        })
      }
      </div>
    );
  },
});

export default MetricContainer;
