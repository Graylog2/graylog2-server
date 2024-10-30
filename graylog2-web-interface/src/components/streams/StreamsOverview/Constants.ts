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

import { PluginStore } from 'graylog-web-plugin/plugin';
import type Immutable from 'immutable';

import type { Sort } from 'stores/PaginationTypes';

const getStreamDataWarehouseTableElements = PluginStore.exports('dataWarehouse')?.[0]?.getStreamDataWarehouseTableElements;

const getStreamTableElements = (permissions: Immutable.List<string>, isPipelineColumnPermitted: boolean) => {
  const streamDataWarehouseTableElements = getStreamDataWarehouseTableElements?.(permissions);

  const defaultLayout = {
    entityTableId: 'streams',
    defaultPageSize: 20,
    defaultSort: { attributeId: 'title', direction: 'asc' } as Sort,
    defaultDisplayedAttributes: [
      'title',
      'index_set_title',
      'archiving',
      ...(streamDataWarehouseTableElements?.attributeName ? [streamDataWarehouseTableElements.attributeName] : []),
      'rules',
      ...(isPipelineColumnPermitted ? ['pipelines'] : []),
      'outputs',
      'throughput',
      'disabled',
    ],
  };
  const columnOrder = [
    'title',
    'index_set_title',
    'archiving',
    ...(streamDataWarehouseTableElements?.attributeName ? [streamDataWarehouseTableElements.attributeName] : []),
    'rules',
    ...(isPipelineColumnPermitted ? ['pipelines'] : []),
    'outputs',
    'throughput',
    'disabled',
    'created_at',
  ];
  const additionalAttributes = [
    { id: 'index_set_title', title: 'Index Set', sortable: true, permissions: ['indexsets:read'] },
    { id: 'throughput', title: 'Throughput' },
    { id: 'rules', title: 'Rules' },
    ...(isPipelineColumnPermitted ? [{ id: 'pipelines', title: 'Pipelines' }] : []),
    { id: 'outputs', title: 'Outputs' },
    { id: 'archiving', title: 'Archiving' },
    ...(streamDataWarehouseTableElements?.attributes || []),
  ];

  return {
    defaultLayout,
    columnOrder,
    additionalAttributes,
  };
};

export default getStreamTableElements;
