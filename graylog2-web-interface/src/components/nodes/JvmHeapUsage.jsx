import React, {PropTypes} from 'react';
import Reflux from 'reflux';
import { ProgressBar } from 'react-bootstrap';
import numeral from 'numeral';

import { Spinner } from 'components/common';

import MetricsExtractor from 'logic/metrics/MetricsExtractor';

import MetricsStore from 'stores/metrics/MetricsStore';

import ActionsProvider from 'injection/ActionsProvider';
const MetricsActions = ActionsProvider.getActions('Metrics');

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
          <ProgressBar>
            <ProgressBar className="used-memory" now={metrics.usedPercentage}/>
            <ProgressBar className="committed-memory" now={metrics.committedPercentage - metrics.usedPercentage}/>
          </ProgressBar>
        );

        detail = (
          <p>
            The JVM is using{' '}
            <span className="blob used-memory"/>
            <strong> {numeral(metrics.usedMemory).format('0.0 b')}</strong>
            {' '}of{' '}
            <span className="blob committed-memory"/>
            <strong> {numeral(metrics.committedMemory).format('0.0 b')}</strong>
            {' '}heap space and will not attempt to use more than{' '}
            <span className="blob max-memory" style={{border: '1px solid #ccc'}}/>
            <strong> {numeral(metrics.maxMemory).format('0.0 b')}</strong>
          </p>
        );
      }
    } else {
      progressBar = <ProgressBar/>;
      detail = <p><Spinner text="Loading heap usage information..."/></p>;
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
