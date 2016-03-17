import React from 'react';
import String from 'string';

import { Spinner } from 'components/common';

import InjectStore from 'injection/InjectStore';
import InjectActions from 'injection/InjectActions';

import MetricsExtractor from 'logic/metrics/MetricsExtractor';

var MetricContainer = React.createClass({
  propTypes: {
    name: React.PropTypes.string.isRequired,
    zeroOnMissing: React.PropTypes.bool
  },
  mixins: [InjectActions('Metrics'), InjectStore('Metrics')],
  componentWillMount() {
    this.MetricsActions.addGlobal(this.props.name);
  },
  componentWillUnmount() {
    this.MetricsActions.removeGlobal(this.props.name);
  },
  getDefaultProps() {
    return {
      zeroOnMissing: true,
    };
  },

  getInitialState() {
    return {
    };
  },
  render() {
    if (!this.state.metrics) {
      return (<span>Loading...</span>);
    }
    const fullName = this.props.name;
    const throughput = Object.keys(this.state.metrics)
      .map(nodeId => MetricsExtractor.getValuesForNode(this.state.metrics[nodeId], { throughput: fullName }))
      .reduce((one, two) => one.throughput + two.throughput);
    if (this.props.zeroOnMissing && (!throughput || !throughput.throughput)) {
      throughput["throughput"] = 0;
    }
    return (<div>
      {
        React.Children.map(this.props.children, function (child) {
          return React.cloneElement(child, { metric: { full_name: fullName, count: throughput.throughput }})
        })
      }
    </div>);
  }
});

export default MetricContainer;
