/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import PropTypes from 'prop-types';
import React from 'react';
// eslint-disable-next-line no-restricted-imports
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import styled, { css } from 'styled-components';

import ProgressBar, { Bar } from 'components/graylog/ProgressBar';
import { Spinner } from 'components/common';
import NumberUtils from 'util/NumberUtils';
import MetricsExtractor from 'logic/metrics/MetricsExtractor';
import StoreProvider from 'injection/StoreProvider';
import ActionsProvider from 'injection/ActionsProvider';

const MetricsStore = StoreProvider.getStore('Metrics');
const MetricsActions = ActionsProvider.getActions('Metrics');

const NodeHeap = styled.div`
  margin-top: 10px;

  p {
    margin-bottom: 0;
  }
`;

const Blob = styled.span(({ theme }) => css`
  display: inline-block;
  width: 9px;
  height: 9px;
  margin-left: 2px;
  border: 1px solid;

  &.used-memory {
    background-color: ${theme.colors.variant.primary};
    border-color: ${theme.colors.variant.dark.primary};
  }

  &.committed-memory {
    background-color: ${theme.colors.variant.warning};
    border-color: ${theme.colors.variant.dark.warning};
  }

  &.max-memory {
    background-color: ${theme.colors.global.background};
    border-color: ${theme.colors.gray[80]};
  }
`);

const StyledProgressBar = styled(ProgressBar)`
  height: 25px;
  margin-bottom: 5px;

  ${Bar} {
    line-height: 25px;
  }
`;

const JvmHeapUsage = createReactClass({
  displayName: 'JvmHeapUsage',

  propTypes: {
    nodeId: PropTypes.string.isRequired,
  },

  mixins: [Reflux.connect(MetricsStore)],

  UNSAFE_componentWillMount() {
    const { nodeId } = this.props;

    this.metricNames = {
      usedMemory: 'jvm.memory.heap.used',
      committedMemory: 'jvm.memory.heap.committed',
      maxMemory: 'jvm.memory.heap.max',
    };

    Object.keys(this.metricNames).forEach((metricShortName) => MetricsActions.add(nodeId, this.metricNames[metricShortName]));
  },

  componentWillUnmount() {
    const { nodeId } = this.props;

    Object.keys(this.metricNames).forEach((metricShortName) => MetricsActions.remove(nodeId, this.metricNames[metricShortName]));
  },

  _extractMetricValues() {
    const { nodeId } = this.props;
    const { metrics } = this.state;

    if (metrics && metrics[nodeId]) {
      const extractedMetric = MetricsExtractor.getValuesForNode(metrics[nodeId], this.metricNames);
      const { maxMemory, usedMemory, committedMemory } = extractedMetric;

      if (maxMemory) {
        extractedMetric.usedPercentage = maxMemory === 0 ? 0 : Math.ceil((usedMemory / maxMemory) * 100);
        extractedMetric.committedPercentage = maxMemory === 0 ? 0 : Math.ceil((committedMemory / maxMemory) * 100);

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
