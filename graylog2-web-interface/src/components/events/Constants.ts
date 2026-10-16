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
import type { Sort, Attribute } from 'stores/PaginationTypes';
import EventDefinitionPriorityEnum from 'logic/alerts/EventDefinitionPriorityEnum';
import type { MiddleSectionProps } from 'components/common/PaginatedEntityTable/PaginatedEntityTable';
import { ALPHABETICAL_SORT } from 'components/common/PaginatedEntityTable/slicing/slicingConstants';
import TagsFilter from 'components/events/TagsFilter';

export const EVENTS_ENTITY_TABLE_ID = 'events';

export const nonInfoPriorities = Object.keys(EventDefinitionPriorityEnum.properties)
  .reverse()
  .filter((key) => key !== String(EventDefinitionPriorityEnum.INFO));

export const commonEventAttributes: Array<Attribute> = [
  {
    filter_options: [
      ...Object.keys(EventDefinitionPriorityEnum.properties)
        .reverse()
        .map((num) => ({ value: num, title: num })),
    ],
    filterable: true,
    id: 'priority',
    searchable: false,
    sliceable: true,
    sortable: true,
    get title() {
      return i18n.t('alerts_attributes.priority', { ns: 'events' });
    },
    type: 'STRING',
    slice_sort_default: { mode: ALPHABETICAL_SORT, direction: 'desc' },
  },
  {
    id: 'timestamp',
    get title() {
      return i18n.t('alerts_attributes.timestamp', { ns: 'events' });
    },
    type: 'DATE',
    sortable: true,
    filterable: true,
  },
  {
    id: 'event_definition_id',
    get title() {
      return i18n.t('alerts_attributes.event_definition', { ns: 'events' });
    },
    type: 'STRING',
    sortable: false,
    searchable: false,
    filterable: true,
    related_collection: 'event_definitions',
  },
  {
    id: 'event_definition_type',
    get title() {
      return i18n.t('alerts_attributes.event_definition_type', { ns: 'events' });
    },
    type: 'STRING',
    sortable: true,
  },
  {
    id: 'key',
    get title() {
      return i18n.t('alerts_attributes.key', { ns: 'events' });
    },
    type: 'STRING',
    sortable: true,
    searchable: false,
  },
  {
    id: 'group_by_fields',
    get title() {
      return i18n.t('alerts_attributes.group_by_fields', { ns: 'events' });
    },
    sortable: false,
  },
];
export const detailsAttributes: Array<Attribute> = [
  ...commonEventAttributes,
  {
    id: 'remediation_steps',
    get title() {
      return i18n.t('alerts_attributes.event_procedures', { ns: 'events' });
    },
    hidden: true,
    sortable: false,
  },
  {
    id: 'timerange_start',
    get title() {
      return i18n.t('alerts_attributes.aggregation_time_range', { ns: 'events' });
    },
    sortable: true,
    type: 'DATE',
    filterable: true,
  },
  {
    id: 'id',
    get title() {
      return i18n.t('alerts_attributes.id', { ns: 'events' });
    },
    type: 'STRING',
    sortable: true,
    searchable: true,
    filterable: true,
  },
  {
    id: 'fields',
    get title() {
      return i18n.t('alerts_attributes.additional_fields', { ns: 'events' });
    },
    type: 'STRING',
    sortable: false,
  },
];
export const eventsTableSpecificAttributes: Array<Attribute> = [
  {
    id: 'message',
    get title() {
      return i18n.t('alerts_attributes.description', { ns: 'events' });
    },
    type: 'STRING',
    sortable: false,
    searchable: false,
  },
  {
    id: 'alert',
    get title() {
      return i18n.t('alerts_attributes.type', { ns: 'events' });
    },
    type: 'BOOLEAN',
    sortable: true,
    sliceable: true,
    filterable: true,
    filter_options: [
      {
        value: 'false',
        get title() {
          return i18n.t('alerts_types.event', { ns: 'events' });
        },
      },
      {
        value: 'true',
        get title() {
          return i18n.t('alerts_types.alert', { ns: 'events' });
        },
      },
    ],
  },
  {
    id: 'tags',
    get title() {
      return i18n.t('alerts_attributes.tags', { ns: 'events' });
    },
    type: 'STRING',
    sortable: false,
    filterable: true,
    filter_component: TagsFilter,
  },
];
export const additionalAttributes: Array<Attribute> = [...eventsTableSpecificAttributes, ...detailsAttributes];

export const eventsTableElements = {
  defaultLayout: {
    entityTableId: EVENTS_ENTITY_TABLE_ID,
    defaultPageSize: 20,
    defaultSort: { attributeId: 'timestamp', direction: 'desc' } as Sort,
    defaultDisplayedAttributes: [
      'timestamp',
      'alert',
      'priority',
      'message',
      'event_definition_id',
      'key',
      'event_definition_type',
    ],
    defaultColumnOrder: [
      'timestamp',
      'alert',
      'priority',
      'message',
      'event_definition_id',
      'key',
      'event_definition_type',
      'group_by_fields',
      'remediation_steps',
      'id',
      'tags',
      'fields',
      'timerange_start',
    ],
  },
};

export const REPLAY_SESSION_ID_PARAM = 'replaySessionId';

type EventsMetricsAction = {
  id: string;
  component: React.ComponentType<MiddleSectionProps>;
};

declare module 'graylog-web-plugin/plugin' {
  interface PluginExports {
    'events.metrics.actions'?: Array<EventsMetricsAction>;
  }
}
