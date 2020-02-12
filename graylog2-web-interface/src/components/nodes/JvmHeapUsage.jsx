import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import styled from 'styled-components';
import { darken } from 'polished';

import { ProgressBar } from 'components/graylog';
import { Spinner } from 'components/common';

import NumberUtils from 'util/NumberUtils';
import MetricsExtractor from 'logic/metrics/MetricsExtractor';

import StoreProvider from 'injection/StoreProvider';
import ActionsProvider from 'injection/ActionsProvider';
import teinte from 'theme/teinte';


const MetricsStore = StoreProvider.getStore('Metrics');
const MetricsActions = ActionsProvider.getActions('Metrics');

const NodeHeap = styled.div`
  margin-top: 10px;

  p {
    margin-bottom: 0px;
  }
`;

const Blob = styled.span`
  display: inline-block;
  width: 9px;
  height: 9px;
  margin-left: 2px;
  border: 1px solid;

  &.used-memory {
    background-color: ${teinte.tertiary.quattro};
    border-color: ${darken(0.1, teinte.tertiary.quattro)};
  }

  &.committed-memory {
    background-color: ${teinte.tertiary.sei};
    border-color: ${darken(0.1, teinte.tertiary.sei)};
  }

  &.max-memory {
    background-color: ${teinte.secondary.due};
    border-color: ${darken(0.1, teinte.secondary.due)};
  }
`;

const StyledProgressBar = styled(ProgressBar)`
  height: 25px;
  margin-bottom: 5px;
`;

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
    const { nodeId } = this.props;
    const extractedMetrics = this._extractMetricValues();
    const { usedPercentage, committedPercentage, usedMemory, committedMemory, maxMemory } = extractedMetrics;
    let progressBarConfig = [{ value: 0 }];
    let detail = <p><Spinner text="Loading heap usage information..." /></p>;

    if (usedPercentage || committedPercentage) {
      if (Object.keys(extractedMetrics).length === 0) {
        detail = <p>Heap information unavailable.</p>;
      } else {
        progressBarConfig = [
          { value: usedPercentage, bsStyle: 'primary' },
          { value: committedPercentage - usedPercentage, bsStyle: 'warning' },
        ];

        detail = (
          <p>
            The JVM is using{' '}
            <Blob className="used-memory" />
            <strong> {NumberUtils.formatBytes(usedMemory)}</strong>
            {' '}of{' '}
            <Blob className="committed-memory" />
            <strong> {NumberUtils.formatBytes(committedMemory)}</strong>
            {' '}heap space and will not attempt to use more than{' '}
            <Blob className="max-memory" />
            <strong> {NumberUtils.formatBytes(maxMemory)}</strong>
          </p>
        );
      }
    }

    return (
      <NodeHeap data-node-id={nodeId}>
        <StyledProgressBar bars={progressBarConfig} />

        {detail}
      </NodeHeap>
    );
  },
});

export default JvmHeapUsage;
