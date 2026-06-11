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
import type { StreamMetricField } from 'hooks/useStreamMetrics';

export const METRIC_COLUMN_IDS = {
  messageCount: 'message_count',
  avgProcessingTime: 'avg_processing_time_ms',
  maxProcessingTime: 'max_processing_time_ms',
  associatedInputs: 'associated_inputs',
  pipelines: 'pipelines',
  routingPipelines: 'routing_pipelines',
} as const;

export type MetricColumnId = (typeof METRIC_COLUMN_IDS)[keyof typeof METRIC_COLUMN_IDS];

export const METRIC_COLUMN_TITLES: Record<MetricColumnId, string> = {
  [METRIC_COLUMN_IDS.messageCount]: 'Message Count (Last 24H)',
  [METRIC_COLUMN_IDS.avgProcessingTime]: 'Avg Processing Time (Last 15min)',
  [METRIC_COLUMN_IDS.maxProcessingTime]: 'Max Processing Time (Last 15min)',
  [METRIC_COLUMN_IDS.associatedInputs]: 'Associated Inputs (Last 24H)',
  [METRIC_COLUMN_IDS.pipelines]: 'Pipelines',
  [METRIC_COLUMN_IDS.routingPipelines]: 'Routing Pipelines',
};

const REQUIRED_BACKEND_FIELDS: Record<MetricColumnId, Array<StreamMetricField>> = {
  [METRIC_COLUMN_IDS.messageCount]: ['message_count'],
  [METRIC_COLUMN_IDS.avgProcessingTime]: ['avg_processing_time_ms'],
  [METRIC_COLUMN_IDS.maxProcessingTime]: ['max_processing_time_ms'],
  [METRIC_COLUMN_IDS.associatedInputs]: ['associated_inputs'],
  [METRIC_COLUMN_IDS.pipelines]: ['pipelines'],
  [METRIC_COLUMN_IDS.routingPipelines]: ['routing_pipelines'],
};

export type ExtensionMetricFields = Record<string, Array<string>>;

export const backendFieldsForVisibleColumns = (
  visibleColumnIds: Iterable<string>,
  extensionMetricFields: ExtensionMetricFields = {},
): Array<StreamMetricField | string> => {
  const fields = new Set<StreamMetricField | string>();

  for (const id of visibleColumnIds) {
    const required = REQUIRED_BACKEND_FIELDS[id as MetricColumnId];

    if (required) {
      required.forEach((f) => fields.add(f));
    }

    const extensionRequired = extensionMetricFields[id];

    if (extensionRequired) {
      extensionRequired.forEach((f) => fields.add(f));
    }
  }

  return Array.from(fields);
};
