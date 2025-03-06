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
import type { InputStateByNode, InputStates, InputState } from 'stores/inputs/InputStatesStore';
import type { Input } from 'components/messageloaders/Types';
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
  message_errors: {
    failures_indexing: number;
    failures_processing: number;
    failures_inputs_codecs: number;
    dropped_message_occurrence: number;
  }
  stream_message_count: StreamMessageCount[];
};

export type InputNodeStateInfo = {
  detailed_message: string;
  node_id: string;
};

export type InputNodeStates = {
  states: {
    [key in InputState]?: InputNodeStateInfo[];
  };
  total: number;
};

export type StreamMessageCount = {
  stream_name : string,
  stream_id : string,
  count : number
}

export type InputDiagnostics = {
  stream_message_count: StreamMessageCount[];
};

export const metricWithPrefix = (input: Input, metric: string) => `${input?.type}.${input?.id}.${metric}`;

const getValueFromMetric = (metric) => {
    if (metric === null || metric === undefined) {
      return undefined;
    }

    switch (metric.type) {
      case 'meter':
        return metric.metric.rate.total;
      case 'gauge':
        return metric.metric.value;
      case 'counter':
        return metric.metric.count;
      default:
        return undefined;
    }
  }

export const fetchInputDiagnostics = (inputId: string): Promise<InputDiagnostics> =>
  fetch<InputDiagnostics>('GET', qualifyUrl(`system/inputs/diagnostics/${inputId}`));

const useInputDiagnosis = (
  inputId: string,
): {
  input: Input;
  inputNodeStates: InputNodeStates;
  inputMetrics: InputDiagnosisMetrics;
} => {
  const { input } = useStore(InputsStore);

  useEffect(() => {
    InputsActions.get(inputId);
  }, [inputId]);

  const { data: messageCountByStream } = useQuery<InputDiagnostics, Error>(
    ['input-diagnostics', inputId],
    () =>
      defaultOnError(
        fetchInputDiagnostics(inputId),
        'Fetching Input Diagnostics failed with status',
        'Could not fetch Input Diagnostics',
      ),
    { refetchInterval: 5000 },
  );

  const { inputStates } = useStore(InputStatesStore) as { inputStates: InputStates };
  const inputStateByNode = inputStates ? inputStates[inputId] || {} : ({} as InputStateByNode);
  const inputNodeStates = { total: Object.keys(inputStateByNode).length, states: {} };

  Object.values(inputStateByNode).forEach(({ state, detailed_message, message_input: { node: node_id } }) => {
    if (!inputNodeStates.states[state]) {
      inputNodeStates.states[state] = [{ detailed_message, node_id }];
    } else if (Array.isArray(inputNodeStates.states[state])) {
      inputNodeStates.states[state].push({ detailed_message, node_id });
    }
  });

  const failures_indexing = `org.graylog2.inputs.${inputId}.failures.indexing`;
  const failures_processing = `org.graylog2.inputs.${inputId}.failures.processing`;
  const failures_inputs_codecs = `org.graylog2.inputs.${inputId}.failures.input`;
  const dropped_message_occurrence = `org.graylog2.inputs.${inputId}.dropped.message.occurrence`;
  
  const InputDiagnosisMetricNames = useMemo(
    () => [
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
      dropped_message_occurrence,
    ],
    [input, failures_indexing, failures_processing, failures_inputs_codecs, dropped_message_occurrence],
  );

  const { metrics: metricsByNode } = useStore(MetricsStore);

  const aggregateMetrics = () => {
    const result = {};

    if(!metricsByNode) return result;

    InputDiagnosisMetricNames.forEach((metricName) => {
      result[metricName] = Object.keys(metricsByNode).reduce((previous, nodeId) => {
        if (!metricsByNode[nodeId][metricName]) {
          return previous;
        }

        const metricValue = getValueFromMetric(metricsByNode[nodeId][metricName]);

        if (metricValue !== undefined) {
          return Number.isNaN(previous) ? metricValue : previous + metricValue;
        }

        return previous;
      }, NaN);
    })

    return result;
  };

  const aggregatedMetrics = aggregateMetrics();

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
      incomingMessagesTotal:
        (aggregatedMetrics[metricWithPrefix(input, 'incomingMessages')]) || 0,
      emptyMessages: (aggregatedMetrics[metricWithPrefix(input, 'emptyMessages')]) || 0,
      open_connections: (aggregatedMetrics[metricWithPrefix(input, 'open_connections')]),
      total_connections: (aggregatedMetrics[metricWithPrefix(input, 'total_connections')]),
      read_bytes_1sec: (aggregatedMetrics[metricWithPrefix(input, 'read_bytes_1sec')]),
      read_bytes_total: (aggregatedMetrics[metricWithPrefix(input, 'read_bytes_total')]),
      write_bytes_1sec: (aggregatedMetrics[metricWithPrefix(input, 'write_bytes_1sec')]),
      write_bytes_total: (aggregatedMetrics[metricWithPrefix(input, 'write_bytes_total')]),
      message_errors:{
        failures_indexing: (aggregatedMetrics[failures_indexing]) || 0,
        failures_processing: (aggregatedMetrics[failures_processing]) || 0,
        failures_inputs_codecs: (aggregatedMetrics[failures_inputs_codecs]) || 0,
        dropped_message_occurrence: aggregatedMetrics[dropped_message_occurrence],
      },
      stream_message_count: messageCountByStream?.stream_message_count || [],
    },
  };
};

export default useInputDiagnosis;

