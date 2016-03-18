import React from 'react';

import InjectStore from 'injection/InjectStore';
import InjectActions from 'injection/InjectActions';

import MetricsExtractor from 'logic/metrics/MetricsExtractor';

const MetricContainer = React.createClass({
  propTypes: {
    name: React.PropTypes.string.isRequired,
    zeroOnMissing: React.PropTypes.bool,
    children: React.PropTypes.array.isRequired,
  },
  mixins: [InjectActions('Metrics'), InjectStore('Metrics')],

  getDefaultProps() {
    return {
      zeroOnMissing: true,
    };
  },

  getInitialState() {
    return {
    };
  },

  componentWillMount() {
    this.MetricsActions.addGlobal(this.props.name);
  },

  componentWillUnmount() {
    this.MetricsActions.removeGlobal(this.props.name);
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
