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
import timestamp from 'components/common/Timestamp';

const getStreamDataWarehouseTableElements = PluginStore.exports('dataWarehouse')?.[0]?.getStreamDataWarehouseTableElements;

const getStreamTableElements = () => {
  const defaultLayout = {
    entityTableId: 'events',
    defaultPageSize: 20,
    defaultSort: { attributeId: 'timestamp', direction: 'desc' } as Sort,
    defaultDisplayedAttributes: [
      'priority',
      'message',
      'key',
      'alert',
      'event_definition_id',
      'timestamp',
    ],
  };
  const columnOrder = [
    'priority',
    'message',
    'key',
    'alert',
    'event_definition_id',
    'timestamp',
  ];
  const additionalAttributes = [
    {
      id: 'priority',
      title: '',
      type: 'NUMBER',
      sortable: false,
      searchable: false,
    },
    {
      id: 'message',
      title: 'Description',
      type: 'STRING',
      sortable: false,
      searchable: false,
    },
    {
      id: 'key',
      title: 'Key',
      type: 'STRING',
      sortable: false,
      searchable: false,
    },
    {
      id: 'alert',
      title: 'Type',
      type: 'BOOLEAN',
      sortable: false,
      searchable: false,
    },
    {
      id: 'event_definition_id',
      title: 'Event Definition',
      type: 'STRING',
      sortable: false,
      searchable: false,
    },
    {
      id: 'timestamp',
      title: 'Timestamp',
      type: 'DATE',
      sortable: false,
      filterable: false,
    },
  ];

  return {
    defaultLayout,
    columnOrder,
    additionalAttributes,
  };
};

export default getStreamTableElements;
