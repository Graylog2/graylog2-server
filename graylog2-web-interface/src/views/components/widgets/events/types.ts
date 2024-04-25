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
import type * as React from 'react';

export type EventListItem = {
  archived: boolean,
  assigned_to: string | null,
  created_at: string,
  event_definition_id: string,
  id: string,
  name: string,
  priority: number,
  status: string | null,
  updated_at: string,
  replay_info: {
    timerange_start: string,
    timerange_end: string,
    query: string,
    streams: Array<string>,
  },
}

export type AttributeFilter = {
  configuration: (filterValues: Array<any>, setFilterValues: (filterValues: Array<any>) => void,) => React.ReactNode,
  renderValue: (values: string) => React.ReactNode,
  valueFromConfig: (value: string) => Array<any>,
  valuesForConfig: (values: Array<any>) => Array<string>,
  multiEdit?: boolean
}

export type EventsListResult = {
  id: string,
  events: Array<EventListItem>,
  totalResults: number,
  type: 'events',
}

export type AttributesFilter = Record<string, AttributeFilter>
