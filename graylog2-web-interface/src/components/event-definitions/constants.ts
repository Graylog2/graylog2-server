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

export const SYSTEM_EVENT_DEFINITION_TYPE = 'system-notifications-v1';
// Matches the backend's tactics_techniques filter on the event definitions list.
export const TACTICS_TECHNIQUES_ATTRIBUTE_ID = 'tactics_techniques';

const getEventDefinitionTableElements = (
  pluggableAttributes?: {
    attributeNames?: Array<string>;
    attributes?: Array<Attribute>;
  },
  tacticsTechniquesAttribute?: Attribute,
) => {
  const defaultLayout = {
    entityTableId: 'event_definitions',
    defaultPageSize: 20,
    defaultSort: { attributeId: 'title', direction: 'asc' } as Sort,
    defaultDisplayedAttributes: [
      'title',
      'description',
      'priority',
      'notifications',
      'scheduling',
      'status',
      'matched_at',
      'tags',
      ...(tacticsTechniquesAttribute ? [tacticsTechniquesAttribute.id] : []),
    ],
    defaultColumnOrder: [
      'title',
      'description',
      'priority',
      'notifications',
      '_entity_source.source',
      'matched_at',
      'status',
      'type',
      'scheduling',
      'tags',
      ...(tacticsTechniquesAttribute ? [tacticsTechniquesAttribute.id] : []),
      ...(pluggableAttributes?.attributeNames || []),
    ],
  };

  // `tags` and the plugin's tactics/techniques attribute are supplied via the fetch response (so
  // they show up in the filter dropdown with their custom filter_component) and are NOT added
  // here, otherwise PaginatedEntityTable would render duplicate columns.
  const additionalAttributes: Array<Attribute> = [
    { id: 'scheduling', title: 'Scheduling', sortable: false },
    { id: 'matched_at', title: 'Last Matched', sortable: true },
    { id: 'notifications', title: 'Notifications', sortable: false },
    ...(pluggableAttributes?.attributes || []),
  ];

  return {
    defaultLayout,
    additionalAttributes,
  };
};

export default getEventDefinitionTableElements;
