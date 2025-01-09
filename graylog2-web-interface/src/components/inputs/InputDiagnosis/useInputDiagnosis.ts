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
import { useQuery } from '@tanstack/react-query';

import { useStore } from 'stores/connect';
import InputStatesStore from 'stores/inputs/InputStatesStore';
import { InputsStore, InputsActions } from 'stores/inputs/InputsStore';
import { MetricsStore, MetricsActions } from 'stores/metrics/MetricsStore';
import type { InputStateByNode, InputStates } from 'stores/inputs/InputStatesStore';
import type { Input } from 'components/messageloaders/Types';
import type { CounterMetric, GaugeMetric, Rate } from 'stores/metrics/MetricsStore';
import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import { defaultOnError } from 'util/conditional/onError';

export type InputDiagnosisMetrics = {
  incomingMessagesTotal: number;
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
  stream_message_count: [string, number][];
}

export type InputNodeStateInfo = {
  detailed_message: string,
  node_id: string,
}

export type InputNodeStates = {
  states: {
    'RUNNING'?: InputNodeStateInfo[],
    'FAILED'?: InputNodeStateInfo[],
    'STOPPED'?: InputNodeStateInfo[],
    'STARTING'?: InputNodeStateInfo[],
    'FAILING'?: InputNodeStateInfo[],
    'SETUP'?: InputNodeStateInfo[],
  }
  total: number;
}

export type InputDiagnostics = {
  stream_message_count: {
    [streamName: string]: number,
  },
}

export const metricWithPrefix = (input: Input, metric: string) => `${input?.type}.${input?.id}.${metric}`;

export const fetchInputDiagnostics = (inputId: string): Promise<InputDiagnostics> => fetch<InputDiagnostics>('GET', qualifyUrl(`system/inputs/diagnostics/${inputId}`));

const useInputDiagnosis = (inputId: string): {
  input: Input,
  inputNodeStates: InputNodeStates,
  inputMetrics: InputDiagnosisMetrics,
} => {
  const { input } = useStore(InputsStore);

  useEffect(() => {
    InputsActions.get(inputId);
  }, [inputId]);

  const { data: messageCountByStream } = useQuery<InputDiagnostics, Error>(
    ['input-diagnostics', inputId],
    () => defaultOnError(fetchInputDiagnostics(inputId), 'Fetching Input Diagnostics failed with status', 'Could not fetch Input Diagnostics'),
    { refetchInterval: 5000 },
  );

  const { inputStates } = useStore(InputStatesStore) as { inputStates: InputStates };
  const inputStateByNode = inputStates ? inputStates[inputId] || {} : {} as InputStateByNode;
  const inputNodeStates = { total: Object.keys(inputStateByNode).length, states: {} };

  Object.values(inputStateByNode).forEach(({ state, detailed_message, message_input: { node: node_id } }) => {
    if (!inputNodeStates.states[state]) {
      inputNodeStates.states[state] = [{ detailed_message, node_id }];
    } else if (Array.isArray(inputNodeStates.states[state])) {
      inputNodeStates.states[state].push({ detailed_message, node_id });
    }
  });

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
  ]), [input, failures_indexing, failures_processing, failures_inputs_codecs]);

  const { metrics: metricsByNode } = useStore(MetricsStore);
  const nodeMetrics = (metricsByNode && input?.node) ? metricsByNode[input?.node] : {};

  useEffect(() => {
    InputDiagnosisMetricNames.forEach((metricName) => MetricsActions.addGlobal(metricName));

    return () => {
      InputDiagnosisMetricNames.forEach((metricName) => MetricsActions.removeGlobal(metricName));
    };
  }, [InputDiagnosisMetricNames]);

  return {
    input,
    inputNodeStates,
    inputMetrics: {
      incomingMessagesTotal: (nodeMetrics[metricWithPrefix(input, 'incomingMessages')]?.metric as Rate)?.rate?.total || 0,
      emptyMessages: (nodeMetrics[metricWithPrefix(input, 'emptyMessages')] as CounterMetric)?.metric?.count || 0,
      open_connections: (nodeMetrics[metricWithPrefix(input, 'open_connections')] as GaugeMetric)?.metric?.value,
      total_connections: (nodeMetrics[metricWithPrefix(input, 'total_connections')] as GaugeMetric)?.metric?.value,
      read_bytes_1sec: (nodeMetrics[metricWithPrefix(input, 'read_bytes_1sec')] as GaugeMetric)?.metric?.value,
      read_bytes_total: (nodeMetrics[metricWithPrefix(input, 'read_bytes_total')] as GaugeMetric)?.metric?.value,
      write_bytes_1sec: (nodeMetrics[metricWithPrefix(input, 'write_bytes_1sec')] as GaugeMetric)?.metric?.value,
      write_bytes_total: (nodeMetrics[metricWithPrefix(input, 'write_bytes_total')] as GaugeMetric)?.metric?.value,
      failures_indexing: (nodeMetrics[failures_indexing]?.metric as Rate)?.rate?.fifteen_minute || 0,
      failures_processing: (nodeMetrics[failures_processing]?.metric as Rate)?.rate?.fifteen_minute || 0,
      failures_inputs_codecs: (nodeMetrics[failures_inputs_codecs]?.metric as Rate)?.rate?.fifteen_minute || 0,
      stream_message_count: Object.entries(messageCountByStream?.stream_message_count || {}),
    },
  };
};

export default useInputDiagnosis;
