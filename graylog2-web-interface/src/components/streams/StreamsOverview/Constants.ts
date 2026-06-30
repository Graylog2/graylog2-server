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

import { METRIC_COLUMN_IDS, METRIC_COLUMN_TITLES } from './metricColumns';

const getStreamTableElements = (
  isPipelineColumnPermitted: boolean,
  extensionAttributes?: {
    attributeNames?: Array<string>;
    defaultDisplayedAttributeNames?: Array<string>;
    attributes?: Array<Attribute>;
  },
) => {
  const defaultLayout = {
    entityTableId: 'streams',
    defaultPageSize: 20,
    defaultSort: { attributeId: 'title', direction: 'asc' } as Sort,
    defaultDisplayedAttributes: [
      'title',
      'index_set_title',
      'rules',
      ...(isPipelineColumnPermitted ? ['pipelines'] : []),
      'outputs',
      'archiving',
      ...(extensionAttributes?.defaultDisplayedAttributeNames || []),
      'destination_filters',
      'disabled',
      'throughput',
    ],
    defaultColumnOrder: [
      'title',
      'index_set_title',
      'rules',
      'outputs',
      'archiving',
      ...(extensionAttributes?.attributeNames || []),
      'destination_filters',
      'disabled',
      'throughput',
      METRIC_COLUMN_IDS.messageCount,
      METRIC_COLUMN_IDS.avgProcessingTime,
      METRIC_COLUMN_IDS.maxProcessingTime,
      METRIC_COLUMN_IDS.associatedInputs,
      ...(isPipelineColumnPermitted ? ['pipelines', METRIC_COLUMN_IDS.routingPipelines] : []),
      'created_at',
    ],
  };

  const additionalAttributes: Array<Attribute> = [
    { id: 'index_set_title', title: 'Index Set', sortable: true, permissions: ['indexsets:read'] },
    { id: 'throughput', title: 'Throughput' },
    { id: 'rules', title: 'Stream Rules' },
    ...(isPipelineColumnPermitted ? [{ id: 'pipelines', title: 'Pipelines' }] : []),
    { id: 'outputs', title: 'Outputs' },
    { id: 'archiving', title: 'Archiving' },
    ...(extensionAttributes?.attributes || []),
    { id: 'destination_filters', title: 'Filter Rules' },
    { id: METRIC_COLUMN_IDS.messageCount, title: METRIC_COLUMN_TITLES[METRIC_COLUMN_IDS.messageCount] },
    { id: METRIC_COLUMN_IDS.avgProcessingTime, title: METRIC_COLUMN_TITLES[METRIC_COLUMN_IDS.avgProcessingTime] },
    { id: METRIC_COLUMN_IDS.maxProcessingTime, title: METRIC_COLUMN_TITLES[METRIC_COLUMN_IDS.maxProcessingTime] },
    { id: METRIC_COLUMN_IDS.associatedInputs, title: METRIC_COLUMN_TITLES[METRIC_COLUMN_IDS.associatedInputs] },
    ...(isPipelineColumnPermitted
      ? [{ id: METRIC_COLUMN_IDS.routingPipelines, title: METRIC_COLUMN_TITLES[METRIC_COLUMN_IDS.routingPipelines] }]
      : []),
  ];

  return {
    defaultLayout,
    additionalAttributes,
  };
};

export default getStreamTableElements;
