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
import type { IndexSetMetricField } from './hooks/useIndexSetMetrics';

export const METRIC_COLUMN_IDS = {
  indexCount: 'index_count',
  documentCount: 'document_count',
  sizeBytes: 'size_bytes',
  deflectorHealth: 'deflector_health',
  fieldCount: 'field_count',
} as const satisfies Record<string, IndexSetMetricField>;

export type MetricColumnId = (typeof METRIC_COLUMN_IDS)[keyof typeof METRIC_COLUMN_IDS];

export const METRIC_COLUMN_TITLES: Record<MetricColumnId, string> = {
  [METRIC_COLUMN_IDS.indexCount]: 'Indices',
  [METRIC_COLUMN_IDS.documentCount]: 'Documents',
  [METRIC_COLUMN_IDS.sizeBytes]: 'Size on Disk',
  [METRIC_COLUMN_IDS.deflectorHealth]: 'Deflector',
  [METRIC_COLUMN_IDS.fieldCount]: 'Fields',
};

const METRIC_COLUMN_ID_SET = new Set<string>(Object.values(METRIC_COLUMN_IDS));

export const backendFieldsForVisibleColumns = (visibleColumnIds: Iterable<string>): Array<IndexSetMetricField> =>
  Array.from(new Set(Array.from(visibleColumnIds).filter((id): id is MetricColumnId => METRIC_COLUMN_ID_SET.has(id))));
