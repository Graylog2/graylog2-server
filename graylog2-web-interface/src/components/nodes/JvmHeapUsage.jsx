import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { ProgressBar } from 'components/graylog';
import { Spinner } from 'components/common';

import NumberUtils from 'util/NumberUtils';
import MetricsExtractor from 'logic/metrics/MetricsExtractor';

import StoreProvider from 'injection/StoreProvider';
import ActionsProvider from 'injection/ActionsProvider';

const MetricsStore = StoreProvider.getStore('Metrics');
const MetricsActions = ActionsProvider.getActions('Metrics');

const JvmHeapUsage = createReactClass({
  displayName: 'JvmHeapUsage',

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
    const { nodeId } = this.props;
    const { metrics } = this.state;

    if (metrics && metrics[nodeId]) {
      const extractedMetric = MetricsExtractor.getValuesForNode(metrics[nodeId], this.metricNames);

      if (extractedMetric.maxMemory) {
        extractedMetric.usedPercentage = extractedMetric.maxMemory === 0 ? 0 : (extractedMetric.usedMemory / extractedMetric.maxMemory) * 100;
        extractedMetric.committedPercentage = extractedMetric.maxMemory === 0 ? 0 : (extractedMetric.committedMemory / extractedMetric.maxMemory) * 100;

        return extractedMetric;
      }

      return {
        usedPercentage: 0,
        committedPercentage: 0,
      };
    }

    return {};
  },

  render() {
    let progressBars = [{ value: 0 }];
    let detail = <p><Spinner text="Loading heap usage information..." /></p>;
    const { nodeId } = this.props;
    const extractedMetrics = this._extractMetricValues();

    if (extractedMetrics.usedPercentage || extractedMetrics.committedPercentage) {
      if (Object.keys(extractedMetrics).length === 0) {
        detail = <p>Heap information unavailable.</p>;
      } else {
        progressBars = [
          { value: extractedMetrics.usedPercentage, bsStyle: 'primary' },
          { value: extractedMetrics.committedPercentage - extractedMetrics.usedPercentage, bsStyle: 'warning' },
        ];

        detail = (
          <p>
            The JVM is using{' '}
            <span className="blob used-memory" />
            <strong> {NumberUtils.formatBytes(extractedMetrics.usedMemory)}</strong>
            {' '}of{' '}
            <span className="blob committed-memory" />
            <strong> {NumberUtils.formatBytes(extractedMetrics.committedMemory)}</strong>
            {' '}heap space and will not attempt to use more than{' '}
            <span className="blob max-memory" style={{ border: '1px solid #ccc' }} />
            <strong> {NumberUtils.formatBytes(extractedMetrics.maxMemory)}</strong>
          </p>
        );
      }
    }

    return (
      <div className="graylog-node-heap" data-node-id={nodeId}>
        <ProgressBar bars={progressBars} />

        {detail}
      </div>
    );
  },
});

export default JvmHeapUsage;
