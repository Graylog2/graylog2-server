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
import numeral from 'numeral';

import type { InputSummary } from 'hooks/usePaginatedInputs';

export type InputConnectionMetrics = {
  openConnections: number | undefined;
  totalConnections: number | undefined;
  emptyMessages: number | undefined;
  writtenBytes1Sec: number | undefined;
  writtenBytesTotal: number | undefined;
  readBytes1Sec: number | undefined;
  readBytesTotal: number | undefined;
};

export const getValueFromMetric = (metric) => {
  if (metric === null || metric === undefined) {
    return undefined;
  }

  switch (metric.type) {
    case 'meter':
      return metric.metric.rate.mean;
    case 'gauge':
      return metric.metric.value;
    case 'counter':
      return metric.metric.count;
    default:
      return undefined;
  }
};
export const formatCount = (count: number) => numeral(count).format('0,0');
const inputsMeticNames = [
  'incomingMessages',
  'emptyMessages',
  'open_connections',
  'total_connections',
  'written_bytes_1sec',
  'written_bytes_total',
  'read_bytes_1sec',
  'read_bytes_total',
];

export const prefixMetric = (input: InputSummary, metric: string) => `${input.type}.${input.id}.${metric}`;

export const getMetricNamesForInput = (input: InputSummary) =>
  inputsMeticNames.map((metric) => prefixMetric(input, metric));

export const calculateInputMetrics = (input: InputSummary, metrics: Record<string, any>) => {
  const result: Record<string, number> = {};
  const metricNames = getMetricNamesForInput(input);

  metricNames.forEach((metricName) => {
    result[metricName] = Object.keys(metrics).reduce((previous, nodeId) => {
      if (!metrics[nodeId][metricName]) {
        return previous;
      }

      const value = getValueFromMetric(metrics[nodeId][metricName]);

      if (value !== undefined) {
        return isNaN(previous) ? value : previous + value;
      }

      return previous;
    }, NaN);
  });

  return result;
};

export const calculateInputMetricsByNode = (
  input: InputSummary,
  metrics: Record<string, Record<string, any>>,
): Record<string, Record<string, number>> => {
  const metricNames = getMetricNamesForInput(input);
  const result: Record<string, Record<string, number>> = {};

  for (const [nodeId, nodeMetrics] of Object.entries(metrics)) {
    const perNode: Record<string, number> = {};

    for (const metricName of metricNames) {
      const rawMetric = nodeMetrics[metricName];
      if (!rawMetric) {
        continue;
      }

      const value = getValueFromMetric(rawMetric);
      if (typeof value === 'number' && !isNaN(value)) {
        perNode[metricName] = value;
      }
    }

    if (Object.keys(perNode).length > 0) {
      result[nodeId] = perNode;
    }
  }

  return result;
};

export const getInputConnectionMetrics = (
  input: InputSummary,
  calculatedMetrics: Record<string, number>,
): InputConnectionMetrics => {
  const resolve = (name: string) => calculatedMetrics[prefixMetric(input, name)];

  return {
    openConnections: resolve('open_connections'),
    totalConnections: resolve('total_connections'),
    emptyMessages: resolve('emptyMessages'),
    writtenBytes1Sec: resolve('written_bytes_1sec'),
    writtenBytesTotal: resolve('written_bytes_total'),
    readBytes1Sec: resolve('read_bytes_1sec'),
    readBytesTotal: resolve('read_bytes_total'),
  };
};
