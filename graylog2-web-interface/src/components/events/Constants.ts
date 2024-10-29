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

import type { Sort, Attribute } from 'stores/PaginationTypes';

export const EVENTS_ENTITY_TABLE_ID = 'events';

export const additionalAttributes: Array<Attribute> = [
  {
    id: 'priority',
    title: 'Priority',
    type: 'STRING',
    sortable: true,
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
    sortable: true,
    searchable: false,
  },
  {
    id: 'alert',
    title: 'Type',
    type: 'BOOLEAN',
    sortable: true,
    filterable: true,
    filter_options: [{ value: 'false', title: 'Event' }, { value: 'true', title: 'Alert' }],
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
    sortable: true,
    filterable: true,
  },
  {
    id: 'id',
    title: 'ID',
    type: 'STRING',
    sortable: true,
  },
  {
    id: 'event_definition_type',
    title: 'Event Definition Type',
    type: 'STRING',
    sortable: true,
  },
  {
    id: 'fields',
    title: 'Additional fields',
    type: 'STRING',
    sortable: false,
  },
  {
    id: 'group_by_fields',
    title: 'Group-By Fields',
    sortable: false,
  },
  {
    id: 'remediation_steps',
    title: 'Remediation Steps',
    sortable: false,
  },
];

const getStreamTableElements = () => {
  const defaultLayout = {
    entityTableId: EVENTS_ENTITY_TABLE_ID,
    defaultPageSize: 20,
    defaultSort: { attributeId: 'timestamp', direction: 'desc' } as Sort,
    defaultDisplayedAttributes: [
      'priority',
      'message',
      'key',
      'alert',
      'event_definition_id',
      'event_definition_type',
      'timestamp',
    ],
  };
  const columnOrder = [
    'message',
    'id',
    'priority',
    'key',
    'alert',
    'event_definition_id',
    'event_definition_type',
    'timestamp',
    'fields',
    'group_by_fields',
    'remediation_steps',
  ];

  return {
    defaultLayout,
    columnOrder,
  };
};

export default getStreamTableElements;
