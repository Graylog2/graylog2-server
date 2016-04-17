import React from 'react';
import Reflux from 'reflux';

import StoreProvider from 'injection/StoreProvider';
const MetricsStore = StoreProvider.getStore('Metrics');

import ActionsProvider from 'injection/ActionsProvider';
const MetricsActions = ActionsProvider.getActions('Metrics');

import MetricsExtractor from 'logic/metrics/MetricsExtractor';

const MetricContainer = React.createClass({
  propTypes: {
    name: React.PropTypes.string.isRequired,
    zeroOnMissing: React.PropTypes.bool,
    children: React.PropTypes.array.isRequired,
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
      });
    if (this.props.zeroOnMissing && (!throughput || !throughput.throughput)) {
      throughput = { throughput: 0 };
    }
    return (<div>
      {
        React.Children.map(this.props.children, (child) => {
          return React.cloneElement(child, { metric: { full_name: fullName, count: throughput.throughput } });
        })
      }
    </div>);
  },
});

export default MetricContainer;
