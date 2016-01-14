import React, {PropTypes} from 'react';
import Reflux from 'reflux';
import numeral from 'numeral';

import MetricsExtractor from 'logic/metrics/MetricsExtractor';

import MetricsStore from 'stores/metrics/MetricsStore';

import MetricsActions from 'actions/metrics/MetricsActions';

const JvmHeapUsage = React.createClass({
  propTypes: {
    nodeId: PropTypes.string.isRequired,
  },
  mixins: [Reflux.connect(MetricsStore)],
  componentWillMount() {
    this.metricNames = {
      usedMemory: 'jvm.memory.heap.used',
      committedMemory: 'jvm.memory.heap.committed',
      maxMemory: 'jvm.memory.heap.max',
    };

    Object.keys(this.metricNames).forEach(metricShortName => MetricsActions.add(this.props.nodeId, this.metricNames[metricShortName]));
  },
  componentWillUnmount() {
    Object.keys(this.metricNames).forEach(metricShortName => MetricsActions.remove(this.props.nodeId, this.metricNames[metricShortName]));
  },
  _extractMetricValues() {
    const nodeId = this.props.nodeId;
    const nodeMetrics = this.state.metrics[nodeId];
    const metrics = MetricsExtractor.getValuesForNode(nodeMetrics, this.metricNames);

    metrics.usedPercentage = metrics.maxMemory === 0 ? 0 : (metrics.usedMemory / metrics.maxMemory) * 100;
    metrics.committedPercentage = metrics.maxMemory === 0 ? 0 : (metrics.committedMemory / metrics.maxMemory) * 100;

    return metrics;
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
