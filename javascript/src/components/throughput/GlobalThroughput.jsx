import React from 'react';
import Reflux from 'reflux';
import numeral from 'numeral';

import GlobalThroughputStore from 'stores/metrics/GlobalThroughputStore';

import MetricsActions from 'actions/metrics/MetricsActions';

import { Spinner } from 'components/common';

const GlobalThroughput = React.createClass({
  mixins: [Reflux.connect(GlobalThroughputStore)],
  componentDidMount() {
    setInterval(MetricsActions.list, 2000);
  },
  metrics: ['org.graylog2.throughput.input.1-sec-rate', 'org.graylog2.throughput.output.1-sec-rate'],
  render() {
    if (!this.state.throughput) {
      return <Spinner />;
    }
    return (
      <span>
        In <strong className="total-throughput">{numeral(this.state.throughput.input).format('0,0')}</strong>{' '}
        / Out <strong className="total-throughput">{numeral(this.state.throughput.output).format('0,0')}</strong> msg/s
      </span>
    );
  },
});

module.exports = GlobalThroughput;
