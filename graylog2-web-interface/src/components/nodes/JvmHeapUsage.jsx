import React, {PropTypes} from 'react';
import Reflux from 'reflux';
import numeral from 'numeral';

import MetricsStore from 'stores/metrics/MetricsStore';

import MetricsActions from 'actions/metrics/MetricsActions';

const HEAP_USED_INDEX = 0;
const HEAP_COMMITTED_INDEX = 1;
const HEAP_MAX_INDEX = 2;

const JvmHeapUsage = React.createClass({
  propTypes: {
    nodeId: PropTypes.string.isRequired,
  },
  mixins: [Reflux.connect(MetricsStore)],
  componentWillMount() {
    this.metricNames = [
      'jvm.memory.heap.used',
      'jvm.memory.heap.committed',
      'jvm.memory.heap.max',
    ];

    this.metricNames.forEach(metricName => MetricsActions.add(this.props.nodeId, metricName));
  },
  componentWillUnmount() {
    this.metricNames.forEach(metricName => MetricsActions.remove(this.props.nodeId, metricName));
  },
  _extractMetricValues() {
    const nodeId = this.props.nodeId;
    const nodeMetrics = this.state.metrics[nodeId];
    if (nodeMetrics === null || nodeMetrics === undefined || Object.keys(nodeMetrics).length === 0) {
      return {};
    }

    let used = 0;
    let committed = 0;
    let max = 0;

    const usedMetricObject = nodeMetrics[this.metricNames[HEAP_USED_INDEX]];
    if (usedMetricObject) {
      used = usedMetricObject.metric.value;
    }

    const committedMetricObject = nodeMetrics[this.metricNames[HEAP_COMMITTED_INDEX]];
    if (committedMetricObject) {
      committed = committedMetricObject.metric.value;
    }

    const maxMetricObject = nodeMetrics[this.metricNames[HEAP_MAX_INDEX]];
    if (maxMetricObject) {
      max = maxMetricObject.metric.value;
    }

    const usedPercentage = max === 0 ? 0 : (used / max) * 100;
    const committedPercentage = max === 0 ? 0 : (committed / max) * 100;

    return {
      usedMemory: used,
      usedPercentage: usedPercentage,
      committedMemory: committed,
      committedPercentage: committedPercentage,
      maxMemory: max,
    };
  },
  render() {
    let progressBar;
    let detail;

    if (this.state.metrics) {
      const metrics = this._extractMetricValues();

      if (Object.keys(metrics).length === 0) {
        progressBar = <div className="progress"></div>;
        detail = <p>Heap information unavailable.</p>;
      } else {
        progressBar = (
          <div className="progress">
            <div className="progress-bar heap-used-percent" style={{width: `${metrics.usedPercentage}%`}}></div>
            <div className="progress-bar progress-bar-warning heap-total-percent"
                 style={{width: `${(metrics.committedPercentage - metrics.usedPercentage)}%`}}></div>
          </div>
        );

        detail = (
          <p>
            The JVM is using{' '}
            <span className="blob" style={{backgroundColor: '#9e1f63'}}/>
            <strong><span className="heap-used"> {numeral(metrics.usedMemory).format('0.0 b')}</span></strong>
            {' '}of{' '}
            <span className="blob" style={{backgroundColor: '#f7941e'}}/>
            <strong><span className="heap-total"> {numeral(metrics.committedMemory).format('0.0 b')}</span></strong>
            {' '}heap space and will not attempt to use more than{' '}
            <span className="blob" style={{backgroundColor: '#f5f5f5'}}/>
            <strong><span className="heap-max"> {numeral(metrics.maxMemory).format('0.0 b')}</span></strong>
          </p>
        );
      }
    } else {
      progressBar = <div className="progress"></div>;
      detail = <p><i className="fa fa-spin fa-spinner"/> Loading heap usage information...</p>;
    }

    return (
      <div className="graylog-node-heap" data-node-id={this.props.nodeId}>
        {progressBar}

        {detail}
      </div>
    );
  },
});

export default JvmHeapUsage;
