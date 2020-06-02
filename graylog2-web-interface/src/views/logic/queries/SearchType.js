// @flow strict
import type MessageSortConfig from 'views/logic/searchtypes/messages/MessageSortConfig';
import type SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import type { Decorator } from 'views/components/messagelist/decorators/Types';
import type { Interval } from 'views/components/aggregationbuilder/pivottypes/Interval';
import type { ElasticsearchQueryString, TimeRange } from './Query';

type SearchTypePivot = {
  type: string,
  field: string,
  limit?: number,
  interval?: Interval,
};

type SearchTypeBase = {
  filter: ?string,
  id: string,
  name: ?string,
  query: ?ElasticsearchQueryString,
  timerange: ?TimeRange,
  type: string,
  streams: Array<string>,
};

export type AggregationSearchType = SearchTypeBase & {
  sort: Array<SortConfig>,
  series: Array<{id: string, type: string, field: string}>,
  column_groups: Array<SearchTypePivot>,
  row_groups: Array<SearchTypePivot>,
  rollup: boolean,
};

export type MessagesSearchType = SearchTypeBase & {
  sort: Array<MessageSortConfig>,
  decorators: Array<Decorator>,
  limit: number,
  offset: number,
};

export type SearchType = AggregationSearchType | MessagesSearchType;
