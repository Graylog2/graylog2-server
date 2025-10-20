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

import React, { useEffect } from 'react';

import NumberUtils from 'util/NumberUtils';
import { Icon, Spinner } from 'components/common';
import type { InputSummary } from 'hooks/usePaginatedInputs';
import { MetricsActions, MetricsStore } from 'stores/metrics/MetricsStore';
import { useStore } from 'stores/connect';

import {
  calculateInputMetrics,
  formatCount,
  getMetricNamesForInput,
  prefixMetric,
} from '../../helpers/InputThroughputUtils';

type Props = {
  input: InputSummary;
};

const Connections = ({ openConnections, totalConnections }: { openConnections: number; totalConnections: number }) => (
  <span>
    Active connections: <span className="active">{formatCount(openConnections)} </span>(
    <span className="total">{formatCount(totalConnections)}</span> total)
    <br />
  </span>
);

const ExpandedThroughputSection = ({ input }: Props) => {
  const metrics = useStore(MetricsStore, (store) => store.metrics);
  const metricNames = getMetricNamesForInput(input);

  useEffect(() => {
    metricNames.map((metricName) => MetricsActions.addGlobal(metricName));

    return () => {
      metricNames.map((metricName) => MetricsActions.removeGlobal(metricName));
    };
  }, [input, metricNames]);

  if (!metrics) {
    return <Spinner />;
  }

  const calculatedMetrics = calculateInputMetrics(input, metrics);
  const openConnections = calculatedMetrics[prefixMetric(input, 'open_connections')];
  const totalConnections = calculatedMetrics[prefixMetric(input, 'total_connections')];
  const emptyMessages = calculatedMetrics[prefixMetric(input, 'emptyMessages')];
  const writtenBytes1Sec = calculatedMetrics[prefixMetric(input, 'written_bytes_1sec')];
  const writtenBytesTotal = calculatedMetrics[prefixMetric(input, 'written_bytes_total')];
  const readBytes1Sec = calculatedMetrics[prefixMetric(input, 'read_bytes_1sec')];
  const readBytesTotal = calculatedMetrics[prefixMetric(input, 'read_bytes_total')];

  return (
    <span>
      {isNaN(writtenBytes1Sec) && isNaN(openConnections) && <i>No metrics available for this input</i>}
      {!isNaN(writtenBytes1Sec) && (
        <span>
          <span>Network IO: </span>
          <span>
            <Icon name="arrow_drop_down" />
            <span>{NumberUtils.formatBytes(readBytes1Sec)} </span>

            <Icon name="arrow_drop_up" />
            <span>{NumberUtils.formatBytes(writtenBytes1Sec)}</span>
          </span>

          <span>
            <span> (total: </span>
            <Icon name="arrow_drop_down" />
            <span>{NumberUtils.formatBytes(readBytesTotal)} </span>

            <Icon name="arrow_drop_up" />
            <span>{NumberUtils.formatBytes(writtenBytesTotal)}</span>
            <span> )</span>
          </span>
        </span>
      )}
      <br />
      {!isNaN(openConnections) && !isNaN(totalConnections) && (
        <Connections openConnections={openConnections} totalConnections={totalConnections} />
      )}
      {!isNaN(emptyMessages) && (
        <span>
          Empty messages discarded: {formatCount(emptyMessages)}
          <br />
        </span>
      )}
    </span>
  );
};

export default ExpandedThroughputSection;
