import React, {PropTypes} from 'react';
import numeral from 'numeral';

import MetricsStore from 'stores/metrics/MetricsStore';
const metricsStore = MetricsStore.instance;

const JvmHeapUsage = React.createClass({
  propTypes: {
    nodeId: PropTypes.string.isRequired,
  },
  getInitialState() {
    return ({
      initialized: false,
      usedMemory: 0,
      usedPercentage: 0,
      committedMemory: 0,
      committedPercentage: 0,
      maxMemory: 0,
      hasError: false,
    });
  },
  componentWillMount() {
    const metricNames = [
      'jvm.memory.heap.used',
      'jvm.memory.heap.committed',
      'jvm.memory.heap.max',
    ];
    metricsStore.listen({
      nodeId: this.props.nodeId,
      metricNames: metricNames,
      callback: (update, hasError) => {
        if (hasError) {
          this.setState({hasError: hasError});
          return;
        }
        // update is [{nodeId, values: [{name, value: {metric}}]} ...]
        // metric can be various different things, depending on metric {type: "GAUGE"|"COUNTER"|"METER"|"TIMER"}

        let used = 0;
        let committed = 0;
        let max = 0;
        // we will only get one result, because we ask for only one node
        update[0].values.forEach((namedMetric) => {
          if (namedMetric.name === 'jvm.memory.heap.used') {
            used = namedMetric.metric.value;
          }
          if (namedMetric.name === 'jvm.memory.heap.committed') {
            committed = namedMetric.metric.value;
          }
          if (namedMetric.name === 'jvm.memory.heap.max') {
            max = namedMetric.metric.value;
          }
        });
        const usedPercentage = max === 0 ? 0 : (used / max) * 100;
        const committedPercentage = max === 0 ? 0 : (committed / max) * 100;
        this.setState({
          initialized: true,
          usedMemory: used,
          usedPercentage: usedPercentage,
          committedMemory: committed,
          committedPercentage: committedPercentage,
          maxMemory: max,
          hasError: hasError,
        });
      },
    });
  },
  render() {
    let detail;
    if (this.state.hasError) {
      detail = <p>Heap information unavailable.</p>;
    } else {
      if (this.state.initialized) {
        detail = (<p>
          The JVM is using <span className="blob" style={{backgroundColor: '#9e1f63'}}/>
          <strong>
            <span className="heap-used"> {numeral(this.state.usedMemory).format('0.0 b')}</span> of <span
            className="blob" style={{backgroundColor: '#f7941e'}}/> <span
            className="heap-total"> {numeral(this.state.committedMemory).format('0.0 b')}</span></strong> heap space and
          will
          not attempt to use more than <span className="blob" style={{backgroundColor: '#f5f5f5'}}/>
          <strong><span className="heap-max"> {numeral(this.state.maxMemory).format('0.0 b')}</span></strong>
        </p>);
      } else {
        detail = (<p><i className="fa fa-spin fa-spinner"/> Loading heap usage information...</p>);
      }
    }

    return (
      <div className="graylog-node-heap" data-node-id={this.props.nodeId}>
        <div className="progress">
          <div className="progress-bar heap-used-percent" style={{width: `${this.state.usedPercentage}%`}}></div>
          <div className="progress-bar progress-bar-warning heap-total-percent"
               style={{width: `${(this.state.committedPercentage - this.state.usedPercentage)}%`}}></div>
        </div>

        {detail}
      </div>
    );
  },
});

export default JvmHeapUsage;
