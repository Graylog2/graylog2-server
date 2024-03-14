import type * as React from 'react';

export type EventListItem = {
  archived: boolean,
  assigned_to: string | null,
  created_at: string,
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
