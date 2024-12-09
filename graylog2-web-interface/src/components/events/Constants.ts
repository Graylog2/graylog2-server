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
import EventDefinitionPriorityEnum from 'logic/alerts/EventDefinitionPriorityEnum';

export const EVENTS_ENTITY_TABLE_ID = 'events';

export const commonEventAttributes: Array<Attribute> = [
  {
    id: 'priority',
    title: 'Priority',
    type: 'STRING',
    sortable: true,
    searchable: false,
    filterable: true,
    filter_options: Object.keys(EventDefinitionPriorityEnum.properties)
      .map((num) => ({ value: num, title: num })),
  },
  {
    id: 'timestamp',
    title: 'Timestamp',
    type: 'DATE',
    sortable: true,
    filterable: true,
  },
  {
    id: 'event_definition_id',
    title: 'Event Definition',
    type: 'STRING',
    sortable: false,
    searchable: false,
    filterable: true,
    related_collection: 'event_definitions',
  },
  {
    id: 'event_definition_type',
    title: 'Event Definition Type',
    type: 'STRING',
    sortable: true,
  },
  {
    id: 'key',
    title: 'Key',
    type: 'STRING',
    sortable: true,
    searchable: false,
  },
  {
    id: 'group_by_fields',
    title: 'Group-By Fields',
    sortable: false,
  },
];
export const detailsAttributes: Array<Attribute> = [
  ...commonEventAttributes,
  {
    id: 'remediation_steps',
    title: 'Remediation Steps',
    sortable: false,
  },
  {
    id: 'timerange_start',
    title: 'Aggregation time range',
    sortable: true,
    type: 'DATE',
    filterable: true,
  },
  {
    id: 'id',
    title: 'ID',
    type: 'STRING',
    sortable: true,
    searchable: true,
    filterable: true,
  },
  {
    id: 'fields',
    title: 'Additional Fields',
    type: 'STRING',
    sortable: false,
  },
];
export const eventsTableSpecificAttributes: Array<Attribute> = [
  {
    id: 'message',
    title: 'Description',
    type: 'STRING',
    sortable: false,
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
];
export const additionalAttributes: Array<Attribute> = [
  ...eventsTableSpecificAttributes,
  ...detailsAttributes,
];

export const eventsTableElements = {
  defaultLayout: {
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
  },
  columnOrder: [
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
    'timerange_start',
  ],
};
