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
import { useEffect, useMemo } from 'react';

import { useStore } from 'stores/connect';
import InputStatesStore from 'stores/inputs/InputStatesStore';
import { InputsStore, InputsActions } from 'stores/inputs/InputsStore';
import { MetricsStore, MetricsActions } from 'stores/metrics/MetricsStore';
import type { InputStateByNode, InputStates } from 'stores/inputs/InputStatesStore';
import type { Input } from 'components/messageloaders/Types';
import type { CounterMetric, GaugeMetric, Rate } from 'stores/metrics/MetricsStore';

export type InputDiagnosisMetrics = {
  incomingMessagesTotal: number;
  incomingMessages15minAvg: number;
  emptyMessages: number;
  open_connections: number;
  total_connections: number;
  read_bytes_1sec: number;
  read_bytes_total: number;
  write_bytes_1sec: number;
  write_bytes_total: number;
  failures_indexing: any;
  failures_processing: any;
  failures_inputs_codecs: any;
}

export type InputNodeStates = {
  running: number;
  failed: number;
  total: number;
}

export const metricWithPrefix = (input: Input, metric: string) => `${input?.type}.${input?.id}.${metric}`;

const useInputDiagnosis = (inputId: string): {
  input: Input,
  inputNodeStates: InputNodeStates,
  inputMetrics: InputDiagnosisMetrics,
} => {
  const { input } = useStore(InputsStore);

  useEffect(() => {
    InputsActions.get(inputId);
  }, [inputId]);

  const { inputStates } = useStore(InputStatesStore) as { inputStates: InputStates };
  const inputStateByNode = inputStates ? inputStates[inputId] || {} : {} as InputStateByNode;
  const inputNodeStates = Object.values(inputStateByNode).map(({ state }) => state);

  const failures_indexing = `org.graylog2.${inputId}.failures.indexing`;
  const failures_processing = `org.graylog2.${inputId}.failures.processing`;
  const failures_inputs_codecs = `org.graylog2.inputs.codecs.*.${inputId}.failures`;

  const InputDiagnosisMetricNames = useMemo(() => ([
    metricWithPrefix(input, 'incomingMessages'),
    metricWithPrefix(input, 'emptyMessages'),
    metricWithPrefix(input, 'open_connections'),
    metricWithPrefix(input, 'total_connections'),
    metricWithPrefix(input, 'written_bytes_1sec'),
    metricWithPrefix(input, 'written_bytes_total'),
    metricWithPrefix(input, 'read_bytes_1sec'),
    metricWithPrefix(input, 'read_bytes_total'),
    metricWithPrefix(input, 'failures.indexing'),
    metricWithPrefix(input, 'failures.processing'),
    failures_indexing,
    failures_processing,
    failures_inputs_codecs,
  ]), [input]);

  const { metrics: metricsByNode } = useStore(MetricsStore);
  console.log(metricsByNode);
  const nodeMetrics = (metricsByNode && input?.node) ? metricsByNode[input?.node] : {};

  useEffect(() => {
    InputDiagnosisMetricNames.forEach((metricName) => MetricsActions.addGlobal(metricName));

    return () => {
      InputDiagnosisMetricNames.forEach((metricName) => MetricsActions.removeGlobal(metricName));
    };
  }, [InputDiagnosisMetricNames]);

  return {
    input,
    inputNodeStates: {
      running: inputNodeStates.filter((state) => state === 'RUNNING').length,
      failed: inputNodeStates.filter((state) => state === 'FAILED').length,
      total: inputNodeStates.length,
    },
    inputMetrics: {
      incomingMessagesTotal: (nodeMetrics[metricWithPrefix(input, 'incomingMessages')]?.metric as Rate)?.rate?.total || 0,
      incomingMessages15minAvg: (nodeMetrics[metricWithPrefix(input, 'incomingMessages')]?.metric as Rate)?.rate?.fifteen_minute || 0,
      emptyMessages: (nodeMetrics[metricWithPrefix(input, 'emptyMessages')] as CounterMetric)?.metric?.count || 0,
      open_connections: (nodeMetrics[metricWithPrefix(input, 'open_connections')] as GaugeMetric)?.metric?.value || 0,
      total_connections: (nodeMetrics[metricWithPrefix(input, 'total_connections')] as GaugeMetric)?.metric?.value || 0,
      read_bytes_1sec: (nodeMetrics[metricWithPrefix(input, 'read_bytes_1sec')] as GaugeMetric)?.metric?.value || 0,
      read_bytes_total: (nodeMetrics[metricWithPrefix(input, 'read_bytes_total')] as GaugeMetric)?.metric?.value || 0,
      write_bytes_1sec: (nodeMetrics[metricWithPrefix(input, 'write_bytes_1sec')] as GaugeMetric)?.metric?.value || 0,
      write_bytes_total: (nodeMetrics[metricWithPrefix(input, 'write_bytes_total')] as GaugeMetric)?.metric?.value || 0,
      failures_indexing: nodeMetrics[failures_indexing],
      failures_processing: nodeMetrics[failures_processing],
      failures_inputs_codecs: nodeMetrics[failures_inputs_codecs],
    },
  };
};

export default useInputDiagnosis;
