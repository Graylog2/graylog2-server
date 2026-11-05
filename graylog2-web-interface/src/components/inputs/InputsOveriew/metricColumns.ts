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
import i18n from 'i18n';
import type { InputMetricField } from 'hooks/useInputMetrics';

export const METRIC_COLUMN_IDS = {
  messagesPerStream: 'messages_per_stream',
  extractorCount: 'extractor_count',
  associatedStreams: 'associated_streams',
} as const;

export type MetricColumnId = (typeof METRIC_COLUMN_IDS)[keyof typeof METRIC_COLUMN_IDS];

const METRIC_COLUMN_TITLE_KEYS: Record<MetricColumnId, string> = {
  [METRIC_COLUMN_IDS.messagesPerStream]: 'overview.columns.messages_per_stream',
  [METRIC_COLUMN_IDS.extractorCount]: 'overview.columns.extractor_count',
  [METRIC_COLUMN_IDS.associatedStreams]: 'overview.columns.associated_streams',
};

export const getMetricColumnTitle = (id: MetricColumnId) => i18n.t(METRIC_COLUMN_TITLE_KEYS[id], { ns: 'inputs' });

const REQUIRED_BACKEND_FIELDS: Record<MetricColumnId, Array<InputMetricField>> = {
  [METRIC_COLUMN_IDS.messagesPerStream]: ['messages_per_stream'],
  [METRIC_COLUMN_IDS.extractorCount]: ['extractor_count'],
  [METRIC_COLUMN_IDS.associatedStreams]: ['messages_per_stream'],
};

export const backendFieldsForVisibleColumns = (visibleColumnIds: Iterable<string>): Array<InputMetricField> => {
  const fields = new Set<InputMetricField>();

  for (const id of visibleColumnIds) {
    const required = REQUIRED_BACKEND_FIELDS[id as MetricColumnId];

    if (required) {
      required.forEach((f) => fields.add(f));
    }
  }

  return Array.from(fields);
};
