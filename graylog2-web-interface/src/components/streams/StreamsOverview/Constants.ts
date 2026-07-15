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
import type { Attribute, Sort } from 'stores/PaginationTypes';

import type { ExtensionColumnGroups } from './hooks/useStreamsOverviewExtensions';
import { METRIC_COLUMN_IDS, METRIC_COLUMN_TITLES } from './metricColumns';

export const STREAM_VIEW_VARIANTS = {
  default: '' as const,
  routing: 'routing' as const,
  performance: 'performance' as const,
};

const SHARED_LAYOUT = {
  entityTableId: 'streams',
  defaultPageSize: 20,
  defaultSort: { attributeId: 'title', direction: 'asc' } as Sort,
};

const getStreamTableElements = (
  isPipelineColumnPermitted: boolean,
  extensionAttributes?: {
    attributeNames?: Array<string>;
    attributes?: Array<Attribute>;
  },
  extensionColumnGroups?: ExtensionColumnGroups,
) => {
  const extRouting = extensionColumnGroups?.routing ?? [];
  const extPerformance = extensionColumnGroups?.performance ?? [];

  const groupedIds = new Set([...extRouting, ...extPerformance]);
  const ungroupedExtNames = (extensionAttributes?.attributeNames ?? []).filter((id) => !groupedIds.has(id));

  const defaultCols = [
    'title',
    'index_set_title',
    'rules',
    ...(isPipelineColumnPermitted ? ['pipelines'] : []),
    'destination_filters',
    'disabled',
    'throughput',
  ];

  const routingCols = [
    METRIC_COLUMN_IDS.associatedInputs,
    ...(isPipelineColumnPermitted ? [METRIC_COLUMN_IDS.routingPipelines] : []),
    'outputs',
    ...extRouting,
    'archiving',
  ];

  const performanceCols = [
    METRIC_COLUMN_IDS.messageCount,
    ...extPerformance,
    METRIC_COLUMN_IDS.avgProcessingTime,
    METRIC_COLUMN_IDS.maxProcessingTime,
  ];

  const defaultColumnOrder = [
    ...defaultCols,
    ...routingCols,
    ...performanceCols,
    ...ungroupedExtNames,
    'created_at',
  ];

  const defaultVariantLayout = {
    ...SHARED_LAYOUT,
    defaultColumnOrder,
    defaultDisplayedAttributes: defaultCols,
  };

  const routingVariantLayout = {
    ...SHARED_LAYOUT,
    layoutVariant: STREAM_VIEW_VARIANTS.routing,
    defaultColumnOrder,
    defaultDisplayedAttributes: [...defaultCols, ...routingCols],
  };

  const performanceVariantLayout = {
    ...SHARED_LAYOUT,
    layoutVariant: STREAM_VIEW_VARIANTS.performance,
    defaultColumnOrder,
    defaultDisplayedAttributes: [...defaultCols, ...performanceCols],
  };

  const additionalAttributes: Array<Attribute> = [
    { id: 'index_set_title', title: 'Index Set', sortable: true, permissions: ['indexsets:read'] },
    { id: 'throughput', title: 'Throughput' },
    { id: 'rules', title: 'Stream Rules' },
    ...(isPipelineColumnPermitted ? [{ id: 'pipelines', title: 'Pipelines' }] : []),
    { id: 'destination_filters', title: 'Filter Rules' },
    { id: METRIC_COLUMN_IDS.associatedInputs, title: METRIC_COLUMN_TITLES[METRIC_COLUMN_IDS.associatedInputs] },
    ...(isPipelineColumnPermitted
      ? [{ id: METRIC_COLUMN_IDS.routingPipelines, title: METRIC_COLUMN_TITLES[METRIC_COLUMN_IDS.routingPipelines] }]
      : []),
    { id: 'outputs', title: 'Outputs' },
    ...(extensionAttributes?.attributes || []),
    { id: 'archiving', title: 'Archiving' },
    { id: METRIC_COLUMN_IDS.messageCount, title: METRIC_COLUMN_TITLES[METRIC_COLUMN_IDS.messageCount] },
    { id: METRIC_COLUMN_IDS.avgProcessingTime, title: METRIC_COLUMN_TITLES[METRIC_COLUMN_IDS.avgProcessingTime] },
    { id: METRIC_COLUMN_IDS.maxProcessingTime, title: METRIC_COLUMN_TITLES[METRIC_COLUMN_IDS.maxProcessingTime] },
  ];

  return {
    defaultVariantLayout,
    routingVariantLayout,
    performanceVariantLayout,
    additionalAttributes,
  };
};

export default getStreamTableElements;
