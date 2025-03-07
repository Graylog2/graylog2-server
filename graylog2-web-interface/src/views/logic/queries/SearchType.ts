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
import type MessageSortConfig from 'views/logic/searchtypes/messages/MessageSortConfig';
import type SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import type { Decorator } from 'views/components/messagelist/decorators/Types';
import type { SearchFilter } from 'views/types';
import type { QueryString } from 'views/logic/queries/types';

import type { TimeRange } from './Query';

type AutoInterval = {
  type: 'auto';
  scaling?: number;
};

type TimeUnitInterval = {
  type: 'timeunit';
  value: number;
  unit: string;
};

type Interval = AutoInterval | TimeUnitInterval;

type SearchTypePivot = {
  type: string;
  fields: Array<string>;
  limit?: number;
  interval?: Interval;
};

export type SearchTypeBase = {
  filter: string | undefined | null;
  filters: Array<SearchFilter> | undefined;
  id: string;
  name: string | undefined | null;
  query: QueryString | undefined | null;
  timerange: TimeRange | undefined | null;
  type: string;
  streams: Array<string>;
  stream_categories: Array<string>;
};

export type AggregationSearchType = SearchTypeBase & {
  sort: Array<SortConfig>;
  series: Array<{ id: string; type: string; field: string }>;
  column_groups: Array<SearchTypePivot>;
  row_groups: Array<SearchTypePivot>;
  rollup: boolean;
};

export type MessagesSearchType = SearchTypeBase & {
  sort: Array<MessageSortConfig>;
  decorators: Array<Decorator>;
  limit: number;
  offset: number;
};

export interface PluggableSearchType {
  'aggregation': AggregationSearchType;
  'messages': MessagesSearchType;
}

export type SearchType = PluggableSearchType[keyof PluggableSearchType];
