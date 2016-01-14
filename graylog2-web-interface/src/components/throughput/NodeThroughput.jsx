import React, {PropTypes} from 'react';
import numeral from 'numeral';
import MetricsStore from 'stores/metrics/MetricsStore';

const metricsStore = MetricsStore.instance;

// TODO this is a copy of GlobalTroughput, it just renders differently and only targets a single node.
const NodeThroughput = React.createClass({
  propTypes: {
    nodeId: PropTypes.string.isRequired,
  },
  getInitialState() {
    return {
      initialized: false,
      totalIn: 0,
      totalOut: 0,
      hasError: false,
    };
  },
  componentWillMount() {
    metricsStore.listen({
      nodeId: this.props.nodeId,
      metricNames: ['org.graylog2.throughput.input.1-sec-rate', 'org.graylog2.throughput.output.1-sec-rate'],
      callback: (update, hasError) => {
        if (hasError) {
          this.setState({hasError: hasError});
          return;
        }
        // update is [{nodeId, values: [{name, value: {metric}}]} ...]
        // metric can be various different things, depending on metric {type: "GAUGE"|"COUNTER"|"METER"|"TIMER"}
        let throughIn = 0;
        let throughOut = 0;
        // we will only get 0 or 1 node here.
        update.forEach((perNode) => {
          perNode.values.forEach((namedMetric) => {
            if (namedMetric.name === "org.graylog2.throughput.input.1-sec-rate") {
              throughIn += namedMetric.metric.value;
            } else if (namedMetric.name === "org.graylog2.throughput.output.1-sec-rate") {
              throughOut += namedMetric.metric.value;
            }
          });
        });
        this.setState({initialized: true, totalIn: throughIn, totalOut: throughOut, hasError: hasError});
      },
    });
  },
  render() {
    if (this.state.hasError) {
      return (<span>Unable to load throughput.</span>);
    }
    if (!this.state.initialized) {
      return (<span><i className="fa fa-spin fa-spinner"></i> Loading throughput...</span>);
    }
    return (<span>
            Processing <strong>{numeral(this.state.totalIn).format("0,0")}</strong> incoming and <strong>
      {numeral(this.state.totalOut).format("0,0")}</strong> outgoing msg/s.
        </span>);
  },
});

export default NodeThroughput;
